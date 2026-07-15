import { buildFileDownloadUrl } from '../api/ai.js'

/**
 * 将 Manus 原始步骤结果转为简短可读文案 + 可下载附件
 * 后台推送格式示例：
 * Step3:工具generatePDF返回的结果PDF generated successfully to: D:\...\tmp\pdf\xx.pdf
 *
 * @param {string} raw
 * @returns {{ text: string, files: Array<{ name: string, url: string, type: string }> }}
 */
export function formatManusStep(raw) {
  const text = (raw ?? '').trim()
  if (!text) {
    return { text: '', files: [] }
  }

  // 达到步数上限
  if (text.includes('达到最大步骤')) {
    return { text: '⚠️ 已达到最大执行步数，任务可能未完全完成。', files: [] }
  }

  const stepMatch = text.match(/^Step\s*(\d+)\s*[:：]\s*(.*)$/is)
  const stepNo = stepMatch?.[1]
  const body = stepMatch ? stepMatch[2].trim() : text

  // 可能一步内多个工具结果（用换行拼接）
  const toolBlocks = splitToolBlocks(body)
  if (toolBlocks.length === 0) {
    return {
      text: stepNo ? `🔹 步骤 ${stepNo}：${shorten(body, 80)}` : shorten(body, 120),
      files: [],
    }
  }

  const lines = []
  const files = []

  for (const block of toolBlocks) {
    const parsed = summarizeToolResult(block)
    if (parsed.text) {
      lines.push(stepNo ? `🔹 步骤 ${stepNo}：${parsed.text}` : `🔹 ${parsed.text}`)
    }
    files.push(...parsed.files)
  }

  return {
    text: lines.join('\n'),
    files: dedupeFiles(files),
  }
}

/**
 * @param {string} body
 * @returns {string[]}
 */
function splitToolBlocks(body) {
  // "工具xxx返回的结果yyy"
  const re = /工具([^\n]+?)返回的结果/g
  const indexes = []
  let m
  while ((m = re.exec(body)) !== null) {
    indexes.push({ nameStart: m.index, full: m[0], name: m[1] })
  }
  if (indexes.length === 0) {
    return [body]
  }

  const blocks = []
  for (let i = 0; i < indexes.length; i++) {
    const start = indexes[i].nameStart
    const end = i + 1 < indexes.length ? indexes[i + 1].nameStart : body.length
    blocks.push(body.slice(start, end).trim())
  }
  return blocks
}

/**
 * @param {string} block
 * @returns {{ text: string, files: Array<{ name: string, url: string, type: string }> }}
 */
function summarizeToolResult(block) {
  const match = block.match(/^工具(.+?)返回的结果([\s\S]*)$/)
  const toolName = (match?.[1] || '').trim()
  const result = (match?.[2] || block).trim()
  const name = normalizeToolName(toolName)

  // PDF 成功
  const pdfOk = result.match(/PDF generated successfully to:\s*(.+?)(?:\s*\|\s*download:.*)?$/im)
  if (pdfOk || /generatePDF/i.test(name)) {
    if (/Error generating PDF/i.test(result)) {
      return {
        text: `PDF 生成失败：${shorten(result.replace(/^.*Error generating PDF:\s*/i, ''), 80)}`,
        files: [],
      }
    }
    if (pdfOk) {
      const file = resolveFileFromPath(pdfOk[1].trim(), 'pdf', result)
      return {
        text: file
          ? `已生成约会计划 PDF「${file.name}」，可点击下方按钮下载。`
          : '已生成 PDF 文件。',
        files: file ? [file] : [],
      }
    }
  }

  // 写文件成功
  const fileOk = result.match(/File written successfully:\s*(.+)$/im)
  if (fileOk) {
    const file = resolveFileFromPath(fileOk[1].trim(), 'file', result)
    return {
      text: file ? `已写入文件「${file.name}」。` : '已写入文件。',
      files: file ? [file] : [],
    }
  }

  // 资源下载成功
  const dlOk = result.match(/Resource downloaded successfully to:\s*(.+)$/im)
  if (dlOk) {
    const file = resolveFileFromPath(dlOk[1].trim(), 'download', result)
    return {
      text: file ? `已下载资源「${file.name}」。` : '已下载资源。',
      files: file ? [file] : [],
    }
  }

  if (/doTerminate|terminate/i.test(name) || result.includes('任务结束')) {
    return { text: '任务已完成。', files: [] }
  }

  if (/searchWeb|webSearch/i.test(name)) {
    return { text: '已检索相关信息（餐厅 / 约会地点等）。', files: [] }
  }

  if (/scrapeWebPage|webScraping/i.test(name)) {
    return { text: '已整理网页内容。', files: [] }
  }

  if (/readFile|FileOperation|read/i.test(name)) {
    // 避免把二进制/乱码读出来
    if (looksLikeGarbage(result)) {
      return { text: '已读取本地文件（内容已省略）。', files: [] }
    }
    return { text: `已读取文件：${shorten(result, 60)}`, files: [] }
  }

  if (/terminal|executeTerminal/i.test(name)) {
    if (looksLikeGarbage(result)) {
      return { text: '已执行终端命令（输出已省略）。', files: [] }
    }
    return { text: `已执行命令：${shorten(result, 60)}`, files: [] }
  }

  if (/Error|失败|Exception/i.test(result)) {
    return { text: `${displayToolName(name)}失败：${shorten(result, 80)}`, files: [] }
  }

  if (looksLikeGarbage(result) || result.length > 200) {
    return { text: `已完成「${displayToolName(name)}」。`, files: [] }
  }

  return {
    text: `${displayToolName(name)}：${shorten(result, 80)}`,
    files: [],
  }
}

/**
 * @param {string} absPath
 * @param {'pdf'|'file'|'download'} fallbackType
 * @param {string} rawResult
 */
function resolveFileFromPath(absPath, fallbackType, rawResult) {
  const downloadHint = rawResult.match(/download:\s*type=(\w+)&name=([^\s|"']+)/i)
  if (downloadHint) {
    const type = downloadHint[1]
    const name = cleanFileName(decodeURIComponent(downloadHint[2]))
    return {
      name,
      type,
      url: buildFileDownloadUrl(type, name),
    }
  }

  const normalized = String(absPath || '')
    .replace(/\\/g, '/')
    .split('|')[0]
    .trim()
  const typeMatch = normalized.match(/\/tmp\/(pdf|file|download)\/([^/]+)$/i)
  const type = typeMatch?.[1] || fallbackType
  const name = cleanFileName(typeMatch?.[2] || normalized.split('/').pop())
  if (!name) return null

  return {
    name,
    type,
    url: buildFileDownloadUrl(type, name),
  }
}

function cleanFileName(name) {
  if (!name) return ''
  return name.replace(/^["']+|["']+$/g, '').trim()
}

function normalizeToolName(name) {
  return name.replace(/[（(].*$/, '').trim()
}

function displayToolName(name) {
  const map = {
    generatePDF: '生成 PDF',
    searchWeb: '联网搜索',
    scrapeWebPage: '网页抓取',
    doTerminate: '结束任务',
  }
  return map[name] || name || '工具调用'
}

function shorten(text, max) {
  const t = (text || '').replace(/\s+/g, ' ').trim()
  if (t.length <= max) return t
  return `${t.slice(0, max)}…`
}

function looksLikeGarbage(text) {
  if (!text) return false
  if (text.includes('%PDF-') || text.includes('%%EOF')) return true
  // 不可见/异常比例过高
  const sample = text.slice(0, 400)
  let bad = 0
  for (const ch of sample) {
    const code = ch.charCodeAt(0)
    if (code < 9 || (code > 13 && code < 32) || code === 0xfffd) {
      bad += 1
    }
  }
  return bad / Math.max(sample.length, 1) > 0.08 || sample.length > 120 && bad > 8
}

function dedupeFiles(files) {
  const seen = new Set()
  return files.filter((f) => {
    const key = `${f.type}:${f.name}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

/**
 * 流结束后若已有 PDF 附件，补一句简短总结
 * @param {Array<{ name: string, url: string, type: string }>} files
 * @param {boolean} hadError
 */
export function buildManusClosing(files, hadError) {
  const pdfs = files.filter((f) => f.type === 'pdf' || /\.pdf$/i.test(f.name))
  if (pdfs.length > 0) {
    return `\n\n✅ 七夕约会计划已准备好，请下载 PDF 查看详情。`
  }
  if (hadError) {
    return `\n\n⚠️ 任务执行中出现错误，请根据上方提示重试。`
  }
  return `\n\n✅ 任务处理结束。`
}

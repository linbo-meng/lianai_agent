/**
 * SSE 流式对话接口封装
 * 使用 fetch + ReadableStream 实时消费服务端事件流
 * （Axios 不适合处理浏览器端 SSE 流式响应）
 */

const BASE_URL = '/api'

/**
 * 通用 SSE 请求（按完整事件块解析，而不是逐行回调）
 * @param {string} url 接口路径
 * @param {Record<string, string>} params 查询参数
 * @param {(chunk: string) => void} onMessage 每个完整 SSE 事件回调一次
 * @param {AbortSignal} [signal] 可选中止信号
 * @returns {Promise<void>}
 */
async function streamSse(url, params, onMessage, signal) {
  const query = new URLSearchParams(params).toString()
  const fullUrl = `${BASE_URL}${url}?${query}`

  const response = await fetch(fullUrl, {
    method: 'GET',
    headers: {
      Accept: 'text/event-stream',
    },
    signal,
  })

  if (!response.ok) {
    throw new Error(`SSE 请求失败: ${response.status} ${response.statusText}`)
  }

  if (!response.body) {
    throw new Error('浏览器不支持 ReadableStream')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    buffer = flushSseEvents(buffer, onMessage)
  }

  // 流结束：处理未以空行收尾的最后一块
  const rest = buffer.trim()
  if (rest) {
    const text = extractEventData(rest)
    if (text) {
      onMessage(text)
    }
  }
}

/**
 * 从缓冲中拆出完整 SSE 事件（以空行分隔）并回调
 * @param {string} buffer
 * @param {(chunk: string) => void} onMessage
 * @returns {string} 剩余未完成缓冲
 */
function flushSseEvents(buffer, onMessage) {
  const parts = buffer.split(/\r?\n\r?\n/)
  // 最后一段可能不完整，保留
  const incomplete = parts.pop() ?? ''

  for (const eventBlock of parts) {
    const text = extractEventData(eventBlock)
    if (text) {
      onMessage(text)
    }
  }
  return incomplete
}

/**
 * 从一个 SSE 事件块中提取 data 字段（多行 data 合并）
 * @param {string} eventBlock
 * @returns {string | null}
 */
function extractEventData(eventBlock) {
  if (!eventBlock || !eventBlock.trim()) {
    return null
  }

  const dataLines = []
  for (const rawLine of eventBlock.split(/\r?\n/)) {
    const line = rawLine.trimEnd()
    if (!line || line.startsWith(':')) {
      continue
    }
    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart())
      continue
    }
    // 兼容无 data: 前缀的纯文本推送
    if (!line.startsWith('event:') && !line.startsWith('id:') && !line.startsWith('retry:')) {
      dataLines.push(line)
    }
  }

  if (dataLines.length === 0) {
    return null
  }
  return dataLines.join('\n')
}

/**
 * AI 恋爱大师 - SSE 对话
 */
export function doChatWithLoveAppSse(message, chatId, onMessage, signal) {
  return streamSse(
    '/ai/love_app/chat/sse',
    { message, chatId },
    onMessage,
    signal,
  )
}

/**
 * AI 超级智能体 - SSE 对话
 */
export function doChatWithManus(message, onMessage, signal) {
  return streamSse(
    '/ai/manus/chat',
    { message },
    onMessage,
    signal,
  )
}

/**
 * 构造临时文件下载地址
 * @param {'pdf'|'file'|'download'} type
 * @param {string} name
 */
export function buildFileDownloadUrl(type, name) {
  const query = new URLSearchParams({ type, name }).toString()
  return `${BASE_URL}/files/download?${query}`
}

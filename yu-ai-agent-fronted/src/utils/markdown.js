import { marked } from 'marked'

marked.setOptions({
  breaks: true,
  gfm: true,
})

/**
 * 兼容大模型常见的不规范 Markdown：
 * - ** 加粗内容 **（** 内侧有空格，标准 Markdown 不会识别）
 * - __ 加粗内容 __
 */
function normalizeMarkdown(text) {
  return text
    .replace(/\*\*\s*([^*\n]+?)\s*\*\*/g, (_, inner) => `**${inner.trim()}**`)
    .replace(/__\s*([^_\n]+?)\s*__/g, (_, inner) => `__${inner.trim()}__`)
}

/**
 * 将 Markdown 转为 HTML
 * @param {string} text
 * @returns {string}
 */
export function renderMarkdown(text) {
  if (!text) return ''
  const normalized = normalizeMarkdown(text)
  return marked.parse(normalized, { async: false })
}

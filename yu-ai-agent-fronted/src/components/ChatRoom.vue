<template>
  <div class="chat-room" :class="`theme-${theme}`">
    <header class="chat-header">
      <button class="back-btn" type="button" @click="$router.push('/')">
        ← 返回
      </button>
      <div class="header-info">
        <h1>{{ title }}</h1>
        <p v-if="chatId" class="chat-id">会话 ID：{{ chatId }}</p>
      </div>
    </header>

    <main ref="messageListRef" class="message-list">
      <div v-if="messages.length === 0" class="empty-tip">
        <div class="empty-ring"></div>
        <p>{{ emptyTip }}</p>
      </div>

      <div
        v-for="(msg, index) in messages"
        :key="index"
        class="message-row"
        :class="msg.role"
      >
        <div class="avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
        <div class="bubble">
          <div v-if="msg.content" class="bubble-text" :class="{ markdown: msg.role === 'ai' }">
            <template v-if="msg.role === 'ai'">
              <div class="md-body" v-html="renderMarkdown(msg.content)"></div>
            </template>
            <template v-else>{{ msg.content }}</template>
            <span v-if="msg.streaming" class="cursor">|</span>
          </div>
          <div v-else-if="msg.streaming" class="bubble-text">思考中…</div>

          <div v-if="msg.files?.length" class="file-list">
            <button
              v-for="file in msg.files"
              :key="`${file.type}-${file.name}`"
              type="button"
              class="file-card"
              :disabled="downloadingKey === `${file.type}-${file.name}`"
              @click="handleDownload(file)"
            >
              <span class="file-icon">📄</span>
              <span class="file-meta">
                <span class="file-name">{{ file.name }}</span>
                <span class="file-action">
                  {{
                    downloadingKey === `${file.type}-${file.name}`
                      ? '下载中…'
                      : '点击下载'
                  }}
                </span>
              </span>
            </button>
          </div>
        </div>
      </div>
    </main>

    <footer class="input-area">
      <textarea
        v-model="inputText"
        rows="1"
        :placeholder="placeholder"
        :disabled="loading"
        @keydown.enter.exact.prevent="handleSend"
      />
      <button
        class="send-btn"
        type="button"
        :disabled="loading || !inputText.trim()"
        @click="handleSend"
      >
        {{ loading ? '发送中…' : '发送' }}
      </button>
    </footer>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  theme: {
    type: String,
    default: 'love',
  },
  chatId: {
    type: String,
    default: '',
  },
  emptyTip: {
    type: String,
    default: '开始你的对话吧～',
  },
  placeholder: {
    type: String,
    default: '输入消息，按 Enter 发送',
  },
  sendMessage: {
    type: Function,
    required: true,
  },
})

/** 打字间隔（ms），越小越快 */
const TYPE_INTERVAL = 32
/** 积压较多时单次最多吐出的字数，避免永远追不上 */
const MAX_CHARS_PER_TICK = 4

const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const downloadingKey = ref('')
const messageListRef = ref(null)
let abortController = null
let typewriterTimer = null

watch(
  () =>
    messages.value
      .map((m) => `${m.content}|${m.files?.length ?? 0}`)
      .join(''),
  async () => {
    await nextTick()
    scrollToBottom()
  },
)

onBeforeUnmount(() => {
  stopTypewriter()
  abortController?.abort()
})

function scrollToBottom() {
  const el = messageListRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

/**
 * 通过 fetch + Blob 下载，避免 <a download> 把文件名弄成 download.json
 */
async function handleDownload(file) {
  const key = `${file.type}-${file.name}`
  if (downloadingKey.value === key) return
  downloadingKey.value = key
  try {
    const response = await fetch(file.url)
    if (!response.ok) {
      throw new Error(`下载失败（${response.status}），文件可能不存在`)
    }
    const blob = await response.blob()
    // 若后端误返回 JSON 错误页，给出明确提示
    if (blob.type.includes('json')) {
      const text = await blob.text()
      throw new Error(`下载失败：${text.slice(0, 120)}`)
    }
    const objectUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = file.name || 'file.pdf'
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(objectUrl)
  } catch (err) {
    console.error(err)
    window.alert(err.message || '下载失败，请稍后重试')
  } finally {
    downloadingKey.value = ''
  }
}

function stopTypewriter() {
  if (typewriterTimer != null) {
    clearInterval(typewriterTimer)
    typewriterTimer = null
  }
}

/**
 * 将缓冲区内的文本按打字机节奏追加到指定消息
 */
function startTypewriter(msgIndex, buffer, isStreamDone) {
  stopTypewriter()

  return new Promise((resolve) => {
    typewriterTimer = setInterval(() => {
      if (buffer.text.length > 0) {
        const backlog = buffer.text.length
        const step = Math.min(
          MAX_CHARS_PER_TICK,
          Math.max(1, Math.ceil(backlog / 50)),
        )
        const chunk = buffer.text.slice(0, step)
        buffer.text = buffer.text.slice(step)
        // 必须改 messages 里的响应式对象，否则界面不更新
        messages.value[msgIndex].content += chunk
        return
      }

      if (isStreamDone()) {
        stopTypewriter()
        resolve()
      }
    }, TYPE_INTERVAL)
  })
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  loading.value = true

  messages.value.push({ role: 'ai', content: '', streaming: true, files: [] })
  const aiIndex = messages.value.length - 1

  abortController?.abort()
  abortController = new AbortController()

  const buffer = { text: '' }
  let streamDone = false
  const typeDone = startTypewriter(aiIndex, buffer, () => streamDone)

  try {
    await props.sendMessage(text, {
      onText: (chunk) => {
        buffer.text += chunk ?? ''
      },
      onFiles: (files) => {
        messages.value[aiIndex].files = files ?? []
      },
      signal: abortController.signal,
    })
  } catch (err) {
    if (err?.name === 'AbortError') {
      streamDone = true
      stopTypewriter()
      messages.value[aiIndex].streaming = false
      loading.value = false
      abortController = null
      return
    }

    console.error(err)
    streamDone = true
    await typeDone.catch(() => {})
    if (!messages.value[aiIndex].content && !buffer.text) {
      messages.value[aiIndex].content = `出错了：${err.message || '请稍后重试'}`
    } else if (buffer.text) {
      messages.value[aiIndex].content += buffer.text
      buffer.text = ''
    }
    messages.value[aiIndex].streaming = false
    loading.value = false
    abortController = null
    return
  }

  streamDone = true
  await typeDone

  if (buffer.text) {
    messages.value[aiIndex].content += buffer.text
    buffer.text = ''
  }

  messages.value[aiIndex].streaming = false
  loading.value = false
  abortController = null
}
</script>

<style scoped>
.chat-room {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-width: 880px;
  margin: 0 auto;
  background: var(--color-bg-elevated);
  border-left: 1px solid var(--color-border);
  border-right: 1px solid var(--color-border);
  box-shadow: 0 0 60px rgba(0, 212, 255, 0.06);
}

.theme-love {
  --accent: var(--love-accent);
  --accent-glow: var(--love-glow);
}

.theme-manus {
  --accent: var(--neon-cyan);
  --accent-glow: rgba(0, 212, 255, 0.45);
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
  background: linear-gradient(180deg, rgba(0, 212, 255, 0.06), transparent);
}

.back-btn {
  flex-shrink: 0;
  padding: 8px 14px;
  border-radius: 999px;
  color: var(--color-muted);
  border: 1px solid var(--color-border);
  background: rgba(10, 14, 23, 0.6);
  transition: color 0.2s, border-color 0.2s, box-shadow 0.2s;
}

.back-btn:hover {
  color: var(--neon-cyan);
  border-color: rgba(0, 212, 255, 0.4);
  box-shadow: 0 0 12px rgba(0, 212, 255, 0.2);
}

.header-info h1 {
  font-family: var(--font-sans);
  font-size: 1.05rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--neon-cyan);
  text-shadow: 0 0 12px rgba(0, 212, 255, 0.35);
}

.chat-id {
  margin-top: 2px;
  font-size: 0.72rem;
  color: var(--color-muted);
  word-break: break-all;
  font-family: ui-monospace, monospace;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px 20px;
  background:
    radial-gradient(ellipse at top, rgba(0, 212, 255, 0.05), transparent 45%),
    linear-gradient(180deg, #0d1320, #0a0e17);
}

.empty-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  height: 100%;
  min-height: 220px;
  color: var(--color-muted);
  font-size: 0.95rem;
}

.empty-ring {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 2px solid var(--accent);
  box-shadow: 0 0 20px var(--accent-glow);
  animation: pulseRing 2s ease-in-out infinite;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 18px;
  animation: fadeSlide 0.28s ease;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.72rem;
  font-weight: 700;
  color: #fff;
  background: var(--color-panel);
  border: 1px solid var(--accent);
  box-shadow: 0 0 12px var(--accent-glow);
}

.message-row.user .avatar {
  border-color: var(--neon-blue);
  box-shadow: 0 0 12px rgba(59, 130, 246, 0.4);
  background: #1e3a5f;
}

.bubble {
  max-width: min(72%, 520px);
  padding: 12px 16px;
  border-radius: 14px;
  background: var(--color-panel);
  border: 1px solid var(--color-border);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.25);
}

.message-row.user .bubble {
  background: linear-gradient(135deg, #1d4ed8, #2563eb);
  border-color: transparent;
  box-shadow: 0 0 16px rgba(37, 99, 235, 0.35);
}

.message-row.ai .bubble {
  border-color: color-mix(in srgb, var(--accent) 25%, transparent);
}

.bubble-text {
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 0.95rem;
  line-height: 1.55;
  color: var(--color-ink);
}

.bubble-text.markdown {
  white-space: normal;
}

.md-body :deep(p) {
  margin: 0 0 0.65em;
}

.md-body :deep(p:last-child) {
  margin-bottom: 0;
}

.md-body :deep(strong) {
  font-weight: 700;
  color: #fff;
}

.md-body :deep(em) {
  font-style: italic;
  color: #c5d0e0;
}

.md-body :deep(ul),
.md-body :deep(ol) {
  margin: 0.4em 0 0.65em;
  padding-left: 1.4em;
}

.md-body :deep(li) {
  margin: 0.2em 0;
}

.md-body :deep(code) {
  padding: 0.1em 0.35em;
  border-radius: 4px;
  font-size: 0.88em;
  font-family: ui-monospace, Consolas, monospace;
  background: rgba(0, 212, 255, 0.12);
  color: var(--neon-cyan);
}

.md-body :deep(pre) {
  margin: 0.5em 0;
  padding: 10px 12px;
  overflow-x: auto;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid var(--color-border);
}

.md-body :deep(pre code) {
  padding: 0;
  background: none;
  color: var(--color-ink);
}

.md-body :deep(a) {
  color: var(--neon-cyan);
  text-decoration: underline;
}

.md-body :deep(blockquote) {
  margin: 0.5em 0;
  padding-left: 12px;
  border-left: 3px solid var(--accent);
  color: var(--color-muted);
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.file-card {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(0, 212, 255, 0.28);
  background: rgba(0, 212, 255, 0.08);
  text-align: left;
  color: inherit;
  transition: background 0.2s, border-color 0.2s, box-shadow 0.2s;
}

.file-card:hover:not(:disabled) {
  background: rgba(0, 212, 255, 0.14);
  border-color: rgba(0, 212, 255, 0.5);
  box-shadow: 0 0 16px rgba(0, 212, 255, 0.18);
}

.file-card:disabled {
  opacity: 0.65;
  cursor: wait;
}

.file-icon {
  font-size: 1.25rem;
  line-height: 1;
}

.file-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.file-name {
  font-size: 0.9rem;
  font-weight: 600;
  color: #e8eef8;
  word-break: break-all;
}

.file-action {
  font-size: 0.75rem;
  color: var(--neon-cyan);
}

.cursor {
  display: inline-block;
  margin-left: 1px;
  color: var(--accent);
  font-weight: 400;
  vertical-align: baseline;
  animation: blink 0.8s step-end infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

.input-area {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  padding: 16px 20px 20px;
  border-top: 1px solid var(--color-border);
  background: rgba(10, 14, 23, 0.9);
}

.input-area textarea {
  flex: 1;
  resize: none;
  min-height: 48px;
  max-height: 120px;
  padding: 12px 16px;
  border: 1px solid var(--color-border);
  border-radius: 14px;
  outline: none;
  color: var(--color-ink);
  background: var(--color-panel);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input-area textarea::placeholder {
  color: #5c667a;
}

.input-area textarea:focus {
  border-color: rgba(0, 212, 255, 0.5);
  box-shadow: 0 0 0 3px rgba(0, 212, 255, 0.12), 0 0 16px rgba(0, 212, 255, 0.1);
}

.input-area textarea:disabled {
  opacity: 0.65;
}

.send-btn {
  flex-shrink: 0;
  height: 48px;
  padding: 0 22px;
  border-radius: 999px;
  color: #fff;
  font-weight: 600;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  box-shadow: 0 0 16px rgba(59, 130, 246, 0.4);
  transition: opacity 0.2s, transform 0.15s, box-shadow 0.2s;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 0 24px rgba(59, 130, 246, 0.6);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  box-shadow: none;
}

@keyframes fadeSlide {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes pulseRing {
  0%,
  100% {
    opacity: 0.7;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.08);
  }
}

@media (max-width: 640px) {
  .chat-room {
    max-width: 100%;
    border: none;
  }

  .bubble {
    max-width: 82%;
  }

  .header-info h1 {
    font-size: 0.95rem;
  }
}
</style>

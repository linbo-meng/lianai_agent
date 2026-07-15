<template>
  <ChatRoom
    title="AI 超级智能体"
    theme="manus"
    empty-tip="你好，我是 AI 超级智能体，请告诉我你需要完成的任务～"
    placeholder="描述你的任务…"
    :send-message="handleSend"
  />
</template>

<script setup>
import ChatRoom from '@/components/ChatRoom.vue'
import { doChatWithManus } from '@/api/ai'
import { buildManusClosing, formatManusStep } from '@/utils/manusResult'

async function handleSend(message, { onText, onFiles, signal }) {
  const collectedFiles = []
  let hadError = false

  await doChatWithManus(
    message,
    (raw) => {
      const { text, files } = formatManusStep(raw)
      if (text) {
        onText(`${text}\n`)
        if (/失败|Error|错误/i.test(text)) {
          hadError = true
        }
      }
      if (files.length) {
        for (const f of files) {
          if (!collectedFiles.some((x) => x.type === f.type && x.name === f.name)) {
            collectedFiles.push(f)
          }
        }
        onFiles?.([...collectedFiles])
      }
    },
    signal,
  )

  onText(buildManusClosing(collectedFiles, hadError))
}
</script>

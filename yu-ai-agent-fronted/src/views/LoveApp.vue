<template>
  <ChatRoom
    title="AI 恋爱大师"
    theme="love"
    :chat-id="chatId"
    empty-tip="你好，我是 AI 恋爱大师，有什么情感问题都可以问我～"
    placeholder="聊聊你的心事…"
    :send-message="handleSend"
  />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { v4 as uuidv4 } from 'uuid'
import ChatRoom from '@/components/ChatRoom.vue'
import { doChatWithLoveAppSse } from '@/api/ai'

const chatId = ref('')

onMounted(() => {
  chatId.value = uuidv4()
})

async function handleSend(message, { onText, signal }) {
  await doChatWithLoveAppSse(message, chatId.value, onText, signal)
}
</script>

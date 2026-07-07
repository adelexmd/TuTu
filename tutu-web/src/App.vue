<script setup lang="ts">
import { ref, reactive, nextTick, onMounted } from 'vue'
import { sendMessage, streamMessage, type ChatMode } from './api/chat'

interface Message {
  role: 'user' | 'assistant'
  content: string
  error?: boolean
}

const messages = reactive<Message[]>([])
const inputText = ref('')
const sessionId = ref('')
const mode = ref<ChatMode>('general')
const useStream = ref(true)
const generating = ref(false)
const streamError = ref('')

const listRef = ref<HTMLElement | null>(null)

function newSession() {
  sessionId.value = crypto.randomUUID()
  messages.splice(0, messages.length)
  streamError.value = ''
}

async function scrollToBottom() {
  await nextTick()
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}

function send() {
  const text = inputText.value.trim()
  if (!text || generating.value) return

  messages.push({ role: 'user', content: text })
  inputText.value = ''
  streamError.value = ''
  void scrollToBottom()

  if (useStream.value) {
    sendStream(text)
  } else {
    sendSync(text)
  }
}

function sendStream(text: string) {
  generating.value = true
  const assistant: Message = { role: 'assistant', content: '' }
  messages.push(assistant)

  streamMessage(
    { sessionId: sessionId.value, message: text, mode: mode.value },
    {
      onDelta: (delta: string) => {
        assistant.content += delta
        void scrollToBottom()
      },
      onDone: (sid: string) => {
        sessionId.value = sid
        generating.value = false
      },
      onError: (err: string) => {
        assistant.content = ''
        assistant.error = true
        streamError.value = `流式请求出错：${err}`
        generating.value = false
      },
    },
  )
}

async function sendSync(text: string) {
  generating.value = true
  const assistant: Message = { role: 'assistant', content: '思考中…' }
  messages.push(assistant)
  try {
    const res = await sendMessage({
      sessionId: sessionId.value,
      message: text,
      mode: mode.value,
    })
    sessionId.value = res.sessionId
    assistant.content = res.content
  } catch (e) {
    assistant.content = ''
    assistant.error = true
    streamError.value = `请求出错：${e instanceof Error ? e.message : String(e)}`
  } finally {
    generating.value = false
    void scrollToBottom()
  }
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}

onMounted(() => {
  newSession()
})
</script>

<template>
  <div class="app">
    <header class="header">
      <h1>TuTu 编程教学助手</h1>
      <div class="header-actions">
        <label class="toggle">
          <span>普通</span>
          <input type="checkbox" v-model="useStream" :disabled="generating" />
          <span>流式</span>
        </label>
        <label class="toggle">
          <span>自由对话</span>
          <input
            type="checkbox"
            :checked="mode === 'explain'"
            :disabled="generating"
            @change="mode = ($event.target as HTMLInputElement).checked ? 'explain' : 'general'"
          />
          <span>详细解释</span>
        </label>
        <button class="btn" :disabled="generating" @click="newSession">新建对话</button>
      </div>
    </header>

    <main class="messages" ref="listRef">
      <p v-if="messages.length === 0" class="empty">开始与 TuTu 对话吧～</p>
      <div
        v-for="(m, i) in messages"
        :key="i"
        class="row"
        :class="m.role"
      >
        <div class="bubble" :class="{ error: m.error }">{{ m.content }}</div>
      </div>
    </main>

    <p v-if="streamError" class="error-bar">{{ streamError }}</p>

    <footer class="footer">
      <textarea
        v-model="inputText"
        class="input"
        rows="2"
        placeholder="输入消息，Enter 发送，Shift+Enter 换行"
        :disabled="generating"
        @keydown="onKeydown"
      ></textarea>
      <button class="btn send" :disabled="generating || !inputText.trim()" @click="send">
        {{ generating ? '生成中…' : '发送' }}
      </button>
    </footer>
  </div>
</template>

<style scoped>
.app {
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-width: 860px;
  margin: 0 auto;
  background: #f7f8fa;
  color: #1f2329;
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, 'PingFang SC', sans-serif;
}

.header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 16px;
  background: #ffffff;
  border-bottom: 1px solid #e5e6eb;
}

.header h1 {
  font-size: 18px;
  margin: 0;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toggle {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #4e5969;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty {
  color: #86909c;
  text-align: center;
  margin-top: 24px;
}

.row {
  display: flex;
}

.row.user {
  justify-content: flex-end;
}

.row.assistant {
  justify-content: flex-start;
}

.bubble {
  max-width: 75%;
  padding: 10px 14px;
  border-radius: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
}

.row.user .bubble {
  background: #3370ff;
  color: #ffffff;
  border-bottom-right-radius: 4px;
}

.row.assistant .bubble {
  background: #ffffff;
  color: #1f2329;
  border: 1px solid #e5e6eb;
  border-bottom-left-radius: 4px;
}

.bubble.error {
  background: #fff1f0;
  color: #d4380d;
  border-color: #ffccc7;
}

.error-bar {
  margin: 0;
  padding: 8px 16px;
  background: #fff1f0;
  color: #d4380d;
  font-size: 13px;
  border-top: 1px solid #ffccc7;
}

.footer {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  background: #ffffff;
  border-top: 1px solid #e5e6eb;
}

.input {
  flex: 1;
  resize: none;
  padding: 10px 12px;
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  font-size: 14px;
  font-family: inherit;
  outline: none;
}

.input:focus {
  border-color: #3370ff;
}

.btn {
  padding: 8px 16px;
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  background: #ffffff;
  color: #1f2329;
  font-size: 14px;
  cursor: pointer;
}

.btn:hover:not(:disabled) {
  border-color: #3370ff;
  color: #3370ff;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn.send {
  background: #3370ff;
  color: #ffffff;
  border-color: #3370ff;
  align-self: flex-end;
}

.btn.send:hover:not(:disabled) {
  background: #245bdb;
  color: #ffffff;
}

@media (max-width: 600px) {
  .header h1 {
    font-size: 16px;
  }
  .bubble {
    max-width: 85%;
  }
}
</style>

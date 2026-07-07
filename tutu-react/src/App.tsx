import { useEffect, useRef, useState } from 'react';
import { sendMessage, streamMessage, type ChatMode } from './api/chat';
import './App.css';

interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  error?: boolean;
}

function newSessionId(): string {
  return crypto.randomUUID();
}

export default function App() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [mode, setMode] = useState<ChatMode>('general');
  const [useStream, setUseStream] = useState(true);
  const [sessionId, setSessionId] = useState<string>(() => newSessionId());
  const [busy, setBusy] = useState(false);

  const listRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages]);

  function resetSession() {
    setSessionId(newSessionId());
    setMessages([]);
  }

  function appendMessage(msg: Message) {
    setMessages((prev) => [...prev, msg]);
  }

  function updateMessage(id: string, patch: Partial<Message>) {
    setMessages((prev) => prev.map((m) => (m.id === id ? { ...m, ...patch } : m)));
  }

  function appendToMessage(id: string, delta: string) {
    setMessages((prev) =>
      prev.map((m) => (m.id === id ? { ...m, content: m.content + delta } : m)),
    );
  }

  async function handleSend() {
    const text = input.trim();
    if (!text || busy) return;

    const userMsg: Message = { id: crypto.randomUUID(), role: 'user', content: text };
    appendMessage(userMsg);
    setInput('');
    setBusy(true);

    const assistantId = crypto.randomUUID();
    const req = { sessionId, message: text, mode };

    try {
      if (useStream) {
        appendMessage({ id: assistantId, role: 'assistant', content: '' });
        await streamMessage(req, {
          onDelta: (delta) => appendToMessage(assistantId, delta),
          onDone: (sid) => setSessionId(sid),
          onError: (err) =>
            updateMessage(assistantId, { content: `出错了：${err}`, error: true }),
        });
      } else {
        appendMessage({ id: assistantId, role: 'assistant', content: '思考中…' });
        const result = await sendMessage(req);
        updateMessage(assistantId, { content: result.content });
        setSessionId(result.sessionId);
      }
    } catch (e) {
      const errMsg = e instanceof Error ? e.message : String(e);
      if (useStream) {
        updateMessage(assistantId, { content: `出错了：${errMsg}`, error: true });
      } else {
        updateMessage(assistantId, { content: `出错了：${errMsg}`, error: true });
      }
    } finally {
      setBusy(false);
    }
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      void handleSend();
    }
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>TuTu 编程教学助手</h1>
        <div className="header-controls">
          <label className="switch">
            <span>模式</span>
            <select value={mode} onChange={(e) => setMode(e.target.value as ChatMode)} disabled={busy}>
              <option value="general">自由对话</option>
              <option value="explain">详细解释</option>
            </select>
          </label>
          <label className="switch">
            <span>接口</span>
            <select
              value={useStream ? 'stream' : 'sync'}
              onChange={(e) => setUseStream(e.target.value === 'stream')}
              disabled={busy}
            >
              <option value="stream">流式</option>
              <option value="sync">普通</option>
            </select>
          </label>
          <button className="btn-new" onClick={resetSession} disabled={busy}>
            新建对话
          </button>
        </div>
      </header>

      <main className="messages" ref={listRef}>
        {messages.length === 0 && <p className="empty">开始和 TuTu 对话吧～</p>}
        {messages.map((m) => (
          <div key={m.id} className={`msg-row ${m.role}`}>
            <div className={`bubble ${m.role}${m.error ? ' error' : ''}`}>{m.content}</div>
          </div>
        ))}
      </main>

      <footer className="composer">
        <textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="输入消息，Enter 发送，Shift+Enter 换行"
          rows={3}
          disabled={busy}
        />
        <button className="btn-send" onClick={() => void handleSend()} disabled={busy || !input.trim()}>
          发送
        </button>
      </footer>
    </div>
  );
}

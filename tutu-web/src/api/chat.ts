export type ChatMode = 'general' | 'explain';

export interface ChatRequest {
  sessionId: string;
  message: string;
  mode: ChatMode;
}

const API_BASE = 'http://localhost:8080/api/v1';

export interface ChatResult {
  sessionId: string;
  content: string;
}

// 同步对话
export async function sendMessage(req: ChatRequest): Promise<ChatResult> {
  const res = await fetch(`${API_BASE}/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const json = await res.json();
  if (json.code !== 0) throw new Error(json.message || '请求失败');
  return json.data as ChatResult;
}

// 流式对话（POST + fetch 解析 SSE）
export async function streamMessage(
  req: ChatRequest,
  handlers: {
    onDelta: (delta: string) => void;
    onDone: (sessionId: string) => void;
    onError: (err: string) => void;
  },
): Promise<void> {
  const res = await fetch(`${API_BASE}/chat/stream`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
  if (!res.ok || !res.body) {
    handlers.onError(`HTTP ${res.status}`);
    return;
  }
  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      let idx: number;
      while ((idx = buffer.indexOf('\n\n')) !== -1) {
        const frame = buffer.slice(0, idx);
        buffer = buffer.slice(idx + 2);
        const dataLine = frame.split('\n').find((l) => l.startsWith('data:'));
        if (!dataLine) continue;
        const payload = dataLine.slice(5).trim();
        if (!payload) continue;
        try {
          const chunk = JSON.parse(payload);
          if (chunk.delta) handlers.onDelta(chunk.delta);
          if (chunk.finish) handlers.onDone(chunk.sessionId ?? req.sessionId);
        } catch {
          /* ignore malformed frame */
        }
      }
    }
  } catch (e) {
    handlers.onError(e instanceof Error ? e.message : String(e));
  }
}

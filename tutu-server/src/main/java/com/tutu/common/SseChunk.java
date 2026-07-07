package com.tutu.common;

/**
 * SSE 流式输出单元：data: {"delta": "...", "finish": false, "sessionId": "..."}
 * sessionId 仅在最后一帧（finish=true）回填，便于客户端拿到本次会话的 sessionId。
 */
public class SseChunk {
    private String delta;
    private boolean finish;
    private String sessionId;

    public SseChunk(String delta, boolean finish) {
        this(delta, finish, null);
    }

    public SseChunk(String delta, boolean finish, String sessionId) {
        this.delta = delta;
        this.finish = finish;
        this.sessionId = sessionId;
    }

    public String getDelta() {
        return delta;
    }

    public boolean isFinish() {
        return finish;
    }

    public String getSessionId() {
        return sessionId;
    }
}

package com.tutu.common;

/**
 * 对话请求体。
 * sessionId：会话标识（会话管理为 P1，此处先透传原样返回）
 * message：用户输入
 * mode：general（自由对话）/ explain（代码或概念解释）
 */
public class ChatRequest {
    private String sessionId;
    private String message;
    private String mode;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}

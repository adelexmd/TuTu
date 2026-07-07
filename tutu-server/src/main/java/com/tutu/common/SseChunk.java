package com.tutu.common;

/**
 * SSE 流式输出单元：data: {"delta": "...", "finish": false}
 */
public class SseChunk {
    private String delta;
    private boolean finish;

    public SseChunk(String delta, boolean finish) {
        this.delta = delta;
        this.finish = finish;
    }

    public String getDelta() {
        return delta;
    }

    public boolean isFinish() {
        return finish;
    }
}

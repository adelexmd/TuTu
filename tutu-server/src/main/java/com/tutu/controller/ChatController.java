package com.tutu.controller;

import com.tutu.common.ChatRequest;
import com.tutu.common.Result;
import com.tutu.service.chatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private final chatService chatService;

    @Autowired
    public ChatController(chatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 统一同步对话接口。
     * 请求体：{ "sessionId": "...", "message": "...", "mode": "general|explain" }
     * 响应：{ "code": 0, "data": { "sessionId": "...", "content": "..." }, "message": "success" }
     */
    @PostMapping("/chat")
    public Result<Map<String, String>> chat(@RequestBody ChatRequest req) {
        String content = chatService.sendMessage(req.getMessage(), req.getMode());
        Map<String, String> data = new HashMap<>();
        data.put("sessionId", req.getSessionId() == null ? "" : req.getSessionId());
        data.put("content", content);
        return Result.success(data);
    }

    /**
     * 流式对话接口（SSE）。
     * 返回 SseEmitter，按 data: {"delta":"...","finish":false} 逐 token 推送。
     */
    @PostMapping("/chat/stream")
    public SseEmitter stream(@RequestBody ChatRequest req) {
        return chatService.stream(req.getMessage(), req.getMode());
    }
}

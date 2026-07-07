package com.tutu.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface chatService {
    String sendMessage(String sessionId, String message, String mode);

    SseEmitter stream(String sessionId, String message, String mode);
}

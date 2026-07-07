package com.tutu.service;

import com.tutu.common.ChatRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface chatService {
    String sendMessage(String message, String mode);

    SseEmitter stream(String message, String mode);
}

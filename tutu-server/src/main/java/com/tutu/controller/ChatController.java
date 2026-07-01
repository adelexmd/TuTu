package com.tutu.controller;

import com.tutu.service.chatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/ai")
@RestController
public class ChatController {
    private chatService chatService;
    @Autowired
    public ChatController(chatService chatService) {
        this.chatService = chatService;
    }


    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatService.sendMessage(message);    }
    @GetMapping("/explain")
    public String explain(@RequestParam String message) {
        return chatService.explain(message);    }
    @GetMapping("/explain1")
    public String explain2(@RequestParam String message) {
        return chatService.explain(message);    }
}

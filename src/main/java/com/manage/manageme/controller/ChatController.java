package com.manage.manageme.controller;

import com.manage.manageme.dto.ChatDTOs.*;
import com.manage.manageme.entity.ChatMessage;
import com.manage.manageme.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List; 

@RestController
@RequestMapping("/api/v1.0/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> getHistory() {
        return ResponseEntity.ok(chatService.getChatHistoryForCurrentUser());
    }

    @PostMapping
    public ResponseEntity<ChatResponse> processChat(@RequestBody ChatRequest request) {
        try {
            ChatResponse response = chatService.processUserMessage(request.getMessage(), request.getHistory());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ChatResponse("Sorry, I couldn't process that right now. Can you try again?", null));
        }
    }
}
package com.manage.manageme.controller;

import com.manage.manageme.dto.ChatDTOs.*;
import com.manage.manageme.entity.ChatMessage;
import com.manage.manageme.entity.ChatThread;
import com.manage.manageme.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // 1. Get all chat threads for the current user (ChatGPT style history sidebar)
    @GetMapping("/threads")
    public ResponseEntity<List<ChatThread>> getThreads() {
        return ResponseEntity.ok(chatService.getThreadsForCurrentUser());
    }

    // 2. Get messages for a specific chat thread
    @GetMapping("/thread/{threadId}")
    public ResponseEntity<List<ChatMessage>> getMessages(@PathVariable Long threadId) {
        return ResponseEntity.ok(chatService.getMessagesForThread(threadId));
    }

    // 3. Create a new chat thread
    @PostMapping("/thread")
    public ResponseEntity<ChatThread> createThread(@RequestParam(required = false) String title) {
        return ResponseEntity.ok(chatService.createNewThread(title));
    }

    // 4. Send a message to a specific chat thread
    @PostMapping("/thread/{threadId}")
    public ResponseEntity<ChatResponse> processChat(
            @PathVariable Long threadId, 
            @RequestBody ChatRequest request) {
        try {
            ChatResponse response = chatService.processUserMessage(threadId, request.getMessage());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ChatResponse("Sorry, I couldn't process that right now. Can you try again?", null));
        }
    }
}
package com.manage.manageme.controller;

import com.manage.manageme.dto.ChatDTOs.*;
import com.manage.manageme.entity.ChatMessage;
import com.manage.manageme.entity.ChatThread;
import com.manage.manageme.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1.0/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/threads")
    public ResponseEntity<List<ChatThread>> getThreads() {
        return ResponseEntity.ok(chatService.getThreadsForCurrentUser());
    }

    @GetMapping("/thread/{threadId}")
    public ResponseEntity<List<ChatMessage>> getMessages(@PathVariable Long threadId) {
        return ResponseEntity.ok(chatService.getMessagesForThread(threadId));
    }

    @PostMapping("/thread")
    public ResponseEntity<ChatThread> createThread(@RequestParam(required = false) String title) {
        return ResponseEntity.ok(chatService.createNewThread(title));
    }

    @DeleteMapping("/thread/{threadId}")
    public ResponseEntity<Void> deleteThread(@PathVariable Long threadId) {
        try {
            chatService.deleteThread(threadId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

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
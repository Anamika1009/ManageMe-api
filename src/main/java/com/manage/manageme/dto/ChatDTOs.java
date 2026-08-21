package com.manage.manageme.dto;

import java.util.List;
import java.util.Map;

public class ChatDTOs {
    
    public static class ChatRequest {
        private String message;
        private List<Map<String, String>> history; // Frontend se aayega
        // Getters and Setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public List<Map<String, String>> getHistory() { return history; }
        public void setHistory(List<Map<String, String>> history) { this.history = history; }
    }

    public static class ChatResponse {
        private String reply;
        private ChatAction action;
        public ChatResponse(String reply, ChatAction action) { this.reply = reply; this.action = action; }
        // Getters
        public String getReply() { return reply; }
        public ChatAction getAction() { return action; }
    }

    public static class ChatAction {
        private String type;
        private Map<String, Object> data;
        public ChatAction(String type, Map<String, Object> data) { this.type = type; this.data = data; }
        // Getters
        public String getType() { return type; }
        public Map<String, Object> getData() { return data; }
    }
}
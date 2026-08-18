package com.manage.manageme.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class ReceiptScannerService {

    @Value("${gemini.api.key}")
    private String apiKey;

    public String scanReceipt(MultipartFile file) throws Exception {
        // 1. Convert image to Base64 Data URL format required by Groq
        String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
        String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        String imageDataUrl = "data:" + mimeType + ";base64," + base64Image;
        
        // 2. Setup Groq Chat Completions API URL
        String url = "https://api.groq.com/openai/v1/chat/completions";

        // 3. Prompt for JSON extraction
        String prompt = "Extract details from this receipt in valid JSON matching these exact keys: amount (number), merchant (string), date (YYYY-MM-DD), category (string - choose from Food, Shopping, Transport, Bills, Misc). Do not include markdown or backticks.";

        // 4. Build Request Body natively supporting Groq/OpenAI JSON schema
        Map<String, Object> requestBody = Map.of(
            "model", "qwen/qwen3.6-27b",
            "response_format", Map.of("type", "json_object"),
            "messages", List.of(
                Map.of(
                    "role", "user",
                    "content", List.of(
                        Map.of("type", "text", "text", prompt),
                        Map.of(
                            "type", "image_url", 
                            "image_url", Map.of("url", imageDataUrl)
                        )
                    )
                )
            )
        );

        // 5. Make the API Call with Bearer Authentication
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey); 
        
        ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(requestBody, headers), Map.class);
        
        // 6. Extract the JSON text content from Groq's response structure
        try {
            Map<String, Object> body = response.getBody();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Groq response: " + e.getMessage());
        }
    }
}
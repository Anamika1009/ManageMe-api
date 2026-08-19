package com.manage.manageme.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manage.manageme.dto.ReceiptScanResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;

// Calls Groq's OpenAI-compatible chat completions API (vision model) to
// read a receipt image and extract structured expense data from it.
@Service
public class GeminiReceiptService {

    // Set in application.properties as: groq.api.key=${GROQ_API_KEY}
    @Value("${groq.api.key}")
    private String groqApiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    // Groq's current documented vision-capable model. If Groq deprecates this,
    // check https://console.groq.com/docs/models for the current vision model
    // and swap the value below.
    private static final String VISION_MODEL = "qwen/qwen3.6-27b";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String PROMPT = """
            You are a receipt-reading assistant for an expense tracking app.
            Look at this receipt image and extract the following as STRICT JSON only.
            No markdown, no code fences, no explanation, no extra text — just the JSON object.

            {
              "merchantName": "string, the store or vendor name",
              "amount": number, the final total amount paid (just the number, no currency symbol or commas),
              "date": "YYYY-MM-DD, the transaction date shown on the receipt. If not visible, use today's date.",
              "suggestedCategory": "string, ONE general category: Groceries, Food, Travel, Shopping, Utilities, Entertainment, Health, or Other",
              "icon": "a single emoji that best represents this purchase"
            }

            If this image is not a receipt or is unreadable, respond with exactly:
            { "error": "Could not read this receipt clearly. Try a sharper photo." }
            """;

    public ReceiptScanResponseDTO scanReceipt(MultipartFile file) throws IOException {
        String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
        String mimeType = (file.getContentType() != null) ? file.getContentType() : "image/jpeg";
        String dataUri = "data:" + mimeType + ";base64," + base64Image;

        Map<String, Object> requestBody = Map.of(
                "model", VISION_MODEL,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", List.of(
                                        Map.of("type", "text", "text", PROMPT),
                                        Map.of("type", "image_url", "image_url", Map.of("url", dataUri))
                                )
                        )
                ),
                "temperature", 0.2,
                "response_format", Map.of("type", "json_object")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey.trim());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(GROQ_URL, entity, String.class);

        return parseGroqResponse(response.getBody());
    }

    private ReceiptScanResponseDTO parseGroqResponse(String rawResponse) throws IOException {
        JsonNode root = objectMapper.readTree(rawResponse);
        String text = root.path("choices").get(0).path("message").path("content").asText();

        // Strip markdown code fences in case the model wraps the JSON anyway
        text = text.trim();
        if (text.startsWith("```")) {
            text = text.replaceAll("^```json", "")
                        .replaceAll("^```", "")
                        .replaceAll("```$", "")
                        .trim();
        }

        JsonNode parsed = objectMapper.readTree(text);

        if (parsed.has("error")) {
            throw new RuntimeException(parsed.get("error").asText());
        }

        return ReceiptScanResponseDTO.builder()
                .merchantName(parsed.path("merchantName").asText("Unknown Merchant"))
                .amount(parsed.path("amount").decimalValue())
                .date(parsed.path("date").asText(LocalDate.now().toString()))
                .suggestedCategory(parsed.path("suggestedCategory").asText(""))
                .icon(parsed.path("icon").asText("🧾"))
                .build();
    }
}
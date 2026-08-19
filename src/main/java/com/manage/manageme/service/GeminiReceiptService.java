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

@Service
public class GeminiReceiptService {

    // Set this in application.properties as: gemini.api.key=${GEMINI_API_KEY}
    // and set the GEMINI_API_KEY env var locally + on Render. Never hardcode the key here.
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

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

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", PROMPT),
                                Map.of("inline_data", Map.of(
                                        "mime_type", mimeType,
                                        "data", base64Image
                                ))
                        ))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String url = GEMINI_URL + "?key=" + geminiApiKey;
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        return parseGeminiResponse(response.getBody());
    }

    private ReceiptScanResponseDTO parseGeminiResponse(String rawResponse) throws IOException {
        JsonNode root = objectMapper.readTree(rawResponse);
        String text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

        // Gemini sometimes wraps JSON in ```json ... ``` — strip that if present
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
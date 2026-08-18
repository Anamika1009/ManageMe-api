package com.manage.manageme.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.util.Base64;
import java.util.Map;
import java.util.List;

@Service
public class ReceiptScannerService {

    @Value("${gemini.api.key}")
    private String apiKey;

    public String scanReceipt(MultipartFile file) throws Exception {
        String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        String prompt = "Extract details from this receipt in valid JSON strictly matching keys: amount(number), merchant(string), date(YYYY-MM-DD), category(string). Do not include markdown or backticks.";

        // Construct JSON Payload for Gemini
        Map<String, Object> requestBody = Map.of("contents", List.of(
            Map.of("parts", List.of(
                Map.of("text", prompt),
                Map.of("inline_data", Map.of("mime_type", file.getContentType(), "data", base64Image))
            ))
        ));

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(requestBody, headers), String.class);
        
        return response.getBody(); // Frontend parser me extract kar lenge
    }
}
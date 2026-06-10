package com.manage.manageme.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${spring.mail.from}")
    private String fromEmail;

    private final RestTemplate restTemplate;
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    public EmailService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Plain text email bhejne ke liye (Jaise OTP, Welcome Mail, etc.)
     */
    public void sendEmail(String to, String subject, String body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            Map<String, Object> request = Map.of(
                    "sender", Map.of("email", fromEmail),
                    "to", new Object[]{Map.of("email", to)},
                    "subject", subject,
                    "textContent", body
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(BREVO_API_URL, entity, String.class);

        } catch (Exception e) {
            throw new RuntimeException("Error sending plain text email via Brevo: " + e.getMessage(), e);
        }
    }

    /**
     * Excel sheet attachment ke saath report email bhejne ke liye (Base64 Encoding)
     */
    public void sendEmailWithAttachment(String to, String subject, String body, ByteArrayInputStream fileStream, String fileName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            // 1. Raw binary bytes ko read karke standard Base64 string mein encode karein
            byte[] fileBytes = fileStream.readAllBytes();
            String base64Content = Base64.getEncoder().encodeToString(fileBytes);

            // 2. Brevo API format ke mutabik attachment map object taiyar karein
            Map<String, String> attachmentItem = Map.of(
                    "name", fileName,
                    "content", base64Content
            );

            // 3. Brevo ke JSON schema ke hisab se request body structure set karein
            Map<String, Object> request = Map.of(
                    "sender", Map.of("email", fromEmail),
                    "to", new Object[]{Map.of("email", to)},
                    "subject", subject,
                    "textContent", body,
                    "attachment", List.of(attachmentItem) // JSON Array structure payload
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            // 4. Brevo SMTP REST API endpoint par POST request bhejein
            restTemplate.postForEntity(BREVO_API_URL, entity, String.class);

        } catch (Exception e) {
            throw new RuntimeException("Error sending report email with attachment via Brevo API: " + e.getMessage(), e);
        }
    }
}
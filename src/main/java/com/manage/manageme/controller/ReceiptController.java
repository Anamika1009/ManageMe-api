package com.manage.manageme.controller;

import com.manage.manageme.dto.ReceiptScanResponseDTO;
import com.manage.manageme.service.GeminiReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/receipts")
public class ReceiptController {

    private final GeminiReceiptService geminiReceiptService;

    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> scanReceipt(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No file uploaded."));
        }

        try {
            ReceiptScanResponseDTO result = geminiReceiptService.scanReceipt(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            String message = (e.getMessage() != null) ? e.getMessage() : "Could not read this receipt.";
            return ResponseEntity.unprocessableEntity().body(Map.of("message", message));
        }
    }
}
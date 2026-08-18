package com.manage.manageme.controller; 

// 1. Spring Framework Imports
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// 2. Apne Service class ka import (Package name apne hisaab se check kar lena)
import com.manage.manageme.service.ReceiptScannerService;

@RestController
@RequestMapping("/api/ai")
public class ReceiptController {

    @Autowired
    private ReceiptScannerService scannerService;

    @PostMapping("/scan-receipt")
    public ResponseEntity<String> scan(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(scannerService.scanReceipt(file));
    }
}
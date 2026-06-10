package com.manage.manageme.controller;

import com.manage.manageme.dto.IncomeDTO;
import com.manage.manageme.entity.ProfileEntity;
import com.manage.manageme.service.EmailService;
import com.manage.manageme.service.ExcelService;
import com.manage.manageme.service.IncomeService;
import com.manage.manageme.service.ProfileService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
// application.properties ka context-path (/api/v1.0) iske aage automatic lag jayega.
// Isliye yahan sirf "/reports" rakha hai taaki double URL loop na bane.
@RequestMapping("/reports")
@CrossOrigin(origins = "*")
public class IncomeReportController {

    private final IncomeService incomeService;
    private final ExcelService excelService;
    private final EmailService emailService;
    private final ProfileService profileService;

    public IncomeReportController(IncomeService incomeService, ExcelService excelService,
                                  EmailService emailService, ProfileService profileService) {
        this.incomeService = incomeService;
        this.excelService = excelService;
        this.emailService = emailService;
        this.profileService = profileService;
    }

    /**
     * URL target mapping: GET /api/v1.0/reports/incomes/download
     * Excel sheet client device par download karne ke liye
     */
    @GetMapping("/incomes/download")
    public ResponseEntity<InputStreamResource> downloadIncomeReport() {
        try {
            // Logged-in user ka current month ka data fetch karein
            List<IncomeDTO> dataList = incomeService.getCurrentMonthIncomesForCurrentUser();
            ByteArrayInputStream excelDataStream = excelService.generateIncomeExcelReport(dataList);

            String fileName = "Income_Report_" + LocalDate.now() + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new InputStreamResource(excelDataStream));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * URL target mapping: POST /api/v1.0/reports/incomes/email
     * User ko registered email par Brevo REST API matrix ke throw attachment forward karne ke liye
     */
    @PostMapping("/incomes/email")
    public ResponseEntity<Map<String, String>> emailIncomeReport() {
        try {
            // 1. Dynamic User Validation Profile context load karein
            ProfileEntity currentProfile = profileService.getCurrentProfile();
            String targetRecipientEmail = currentProfile.getEmail();

            // 2. Data retrieve karke dynamic excel sheet taiyar karein
            List<IncomeDTO> dataList = incomeService.getCurrentMonthIncomesForCurrentUser();
            ByteArrayInputStream excelDataStream = excelService.generateIncomeExcelReport(dataList);

            String fileName = "Income_Report_" + LocalDate.now() + ".xlsx";
            String emailSubject = "Financial Inflow Report Export Summary Matrix";

            // Personalized dynamic email body text
            String emailBody = "Hello " + currentProfile.getFullName() + ",\n\nPlease find attached your structural tracking parameters spreadsheet document compiled dynamically from your dashboard data pipelines.";

            // 3. EmailService aur Base64 wrapper engine ke throw mail transfer queue par lagayein
            emailService.sendEmailWithAttachment(targetRecipientEmail, emailSubject, emailBody, excelDataStream, fileName);

            return ResponseEntity.ok(Map.of("message", "Report file dispatched safely to " + targetRecipientEmail));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed executing structural attachment dispatch."));
        }
    }
}
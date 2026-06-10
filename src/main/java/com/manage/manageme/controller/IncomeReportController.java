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
@RequestMapping("/api/v1.0/reports") // FIXED: Mapped to /reports to isolate and eliminate 404 routing conflicts
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

    // Handles layout download button mapping trigger pipeline
    @GetMapping("/incomes/download") // Path structured clean: /api/v1.0/reports/incomes/download
    public ResponseEntity<InputStreamResource> downloadIncomeReport() {
        try {
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

    // Handles the email submission button trigger endpoint pipeline
    @PostMapping("/incomes/email") // Path structured clean: /api/v1.0/reports/incomes/email
    public ResponseEntity<Map<String, String>> emailIncomeReport() {
        try {
            // Logged-in user data dynamically fetch karein
            ProfileEntity currentProfile = profileService.getCurrentProfile();
            String targetRecipientEmail = currentProfile.getEmail();

            List<IncomeDTO> dataList = incomeService.getCurrentMonthIncomesForCurrentUser();
            ByteArrayInputStream excelDataStream = excelService.generateIncomeExcelReport(dataList);

            String fileName = "Income_Report_" + LocalDate.now() + ".xlsx";
            String emailSubject = "Financial Inflow Report Export Summary Matrix";
            String emailBody = "Hello " + currentProfile.getFullName() + ",\n\nPlease find attached your structural tracking parameters spreadsheet document compiled dynamically from your dashboard data pipelines.";

            // Dynamic secure dispatch via Brevo
            emailService.sendEmailWithAttachment(targetRecipientEmail, emailSubject, emailBody, excelDataStream, fileName);

            return ResponseEntity.ok(Map.of("message", "Report file dispatched safely to " + targetRecipientEmail));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed executing structural attachment dispatch."));
        }
    }
}
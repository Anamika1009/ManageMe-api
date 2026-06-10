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
@RequestMapping("/api/v1.0")
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
    @GetMapping("/incomes/report/download")
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
    @PostMapping("/incomes/report/email")
    public ResponseEntity<Map<String, String>> emailIncomeReport() {
        try {
            // 1. Current logged-in user ka profile data nikalen
            ProfileEntity currentProfile = profileService.getCurrentProfile();

            // 2. Us profile se dynamic email extract karein
            String targetRecipientEmail = currentProfile.getEmail();

            // 3. Data aur excel sheet generate karein
            List<IncomeDTO> dataList = incomeService.getCurrentMonthIncomesForCurrentUser();
            ByteArrayInputStream excelDataStream = excelService.generateIncomeExcelReport(dataList);

            String fileName = "Income_Report_" + LocalDate.now() + ".xlsx";
            String emailSubject = "Financial Inflow Report Export Summary Matrix";

            // FIXED: Changed getFullname() to getFullName() matching your Entity field definition
            String emailBody = "Hello " + currentProfile.getFullName() + ",\n\nPlease find attached your structural tracking parameters spreadsheet document compiled dynamically from your dashboard data pipelines.";

            // 4. Dynamic email par send karein
            emailService.sendEmailWithAttachment(targetRecipientEmail, emailSubject, emailBody, excelDataStream, fileName);

            return ResponseEntity.ok(Map.of("message", "Report file dispatched safely to " + targetRecipientEmail));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed executing structural attachment dispatch."));
        }
    }
}
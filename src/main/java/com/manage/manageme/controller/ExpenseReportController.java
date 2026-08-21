package com.manage.manageme.controller;

import com.manage.manageme.dto.ExpenseDTO;
import com.manage.manageme.entity.ProfileEntity;
import com.manage.manageme.service.EmailService;
import com.manage.manageme.service.ExcelService;
import com.manage.manageme.service.ExpenseService;
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
@RequestMapping("/reports")
public class ExpenseReportController {
    private final ExpenseService expenseService;
    private final ExcelService excelService;
    private final EmailService emailService;
    private final ProfileService profileService;

    public ExpenseReportController(ExpenseService expenseService, ExcelService excelService,
                                   EmailService emailService, ProfileService profileService) {
        this.expenseService = expenseService;
        this.excelService = excelService;
        this.emailService = emailService;
        this.profileService = profileService;
    }

    @GetMapping("/expenses/download")
    public ResponseEntity<InputStreamResource> downloadExpenseReport() {
        try {
            List<ExpenseDTO> dataList = expenseService.getCurrentMonthExpensesForUserForReport();
            ByteArrayInputStream excelDataStream = excelService.generateExpenseExcelReport(dataList);
            String fileName = "Expense_Report_" + LocalDate.now() + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new InputStreamResource(excelDataStream));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/expenses/email")
    public ResponseEntity<Map<String, String>> emailExpenseReport() {
        try {
            ProfileEntity currentProfile = profileService.getCurrentProfile();
            List<ExpenseDTO> dataList = expenseService.getCurrentMonthExpensesForUserForReport();
            ByteArrayInputStream excelDataStream = excelService.generateExpenseExcelReport(dataList);
            String fileName = "Expense_Report_" + LocalDate.now() + ".xlsx";
            emailService.sendEmailWithAttachment(
                    currentProfile.getEmail(),
                    "Expense Report Export",
                    "Please find your expense report attached.",
                    excelDataStream,
                    fileName
            );
            return ResponseEntity.ok(Map.of("message", "Expense report sent successfully."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to send expense report."));
        }
    }
}
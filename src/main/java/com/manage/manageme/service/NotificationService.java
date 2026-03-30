package com.manage.manageme.service;

import com.manage.manageme.dto.ExpenseDTO;
import com.manage.manageme.entity.ExpenseEntity;
import com.manage.manageme.entity.ProfileEntity;
import com.manage.manageme.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

// THE FIX: The correct Spring import!
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final EmailService emailService;
    private final ProfileRepository profileRepository;
    private final ExpenseService expenseService;

    @Value("${manageme.frontend.url}")
    private String frontendUrl;


    @Scheduled(cron = "0 0 22 * * *", zone = "IST")
    public void sendDailyIncomeExpenseReminder() {
        log.info("Job started: sendDailyIncomeExpenseReminder()");

        List<ProfileEntity> profiles = profileRepository.findAll();

        for(ProfileEntity profile : profiles) {
            String name = profile.getFullName();
            if (name == null || name.trim().isEmpty()) {
                name = "User";
            }

            String body = "Hi " + name + ",<br><br>"
                    + "This is a friendly reminder to add your income and expenses for today in ManageMe.<br><br>"
                    + "<a href='" + frontendUrl + "' style='display:inline-block; padding:10px 20px; background-color:#4CAF50; color:#ffffff; text-decoration:none; border-radius:5px; font-weight:bold;'>Go to ManageMe</a>"
                    + "<br><br>Best Regards, <br>ManageMe Team";

            emailService.sendEmail(profile.getEmail(), "Daily Reminder: Add Your Income and Expenses", body);
        }
        log.info("Job finished: sendDailyIncomeExpenseReminder()");
    }

    @Scheduled(cron = "0 0 23 * * *", zone = "IST")
    public void sendDailyExpenseSummary() {
        log.info("Job started: sendDailyExpenseSummary()");

        List<ProfileEntity> profiles = profileRepository.findAll();

        for(ProfileEntity profile : profiles) {

            List<ExpenseDTO> todayExpenses = expenseService.getExpensesForUserOnDate(profile.getId(), LocalDate.now());

            if(!todayExpenses.isEmpty()) {
                StringBuilder table = new StringBuilder();

                table.append("<div style='font-family: Arial, Helvetica, sans-serif; color: #333333; width: 100%;'>");

                table.append("<table style='width: 100%; border-collapse: collapse; margin-top: 15px; border: 2px solid #4CAF50;'>");

                // CHANGED: Added border-right to separate header columns and increased padding to 20px
                table.append("<tr style='background-color: #4CAF50; color: #ffffff; text-align: left;'>");
                table.append("<th style='padding: 12px 20px; border-bottom: 2px solid #388E3C; border-right: 1px solid #75C479;'>S.No</th>");
                table.append("<th style='padding: 12px 20px; border-bottom: 2px solid #388E3C; border-right: 1px solid #75C479;'>Name</th>");
                table.append("<th style='padding: 12px 20px; border-bottom: 2px solid #388E3C; border-right: 1px solid #75C479;'>Amount</th>");
                table.append("<th style='padding: 12px 20px; border-bottom: 2px solid #388E3C;'>Category</th>"); // No right border on the last column
                table.append("</tr>");

                int i = 1;
                for(ExpenseDTO expenseDTO : todayExpenses) {
                    table.append("<tr style='background-color: #ffffff; border-bottom: 1px solid #dddddd;'>");

                    // CHANGED: Added border-right to separate data columns and increased padding to 20px
                    table.append("<td style='padding: 12px 20px; color: #777777; border-right: 1px solid #eeeeee;'>").append(i++).append("</td>");
                    table.append("<td style='padding: 12px 20px; font-weight: 500; border-right: 1px solid #eeeeee;'>").append(expenseDTO.getName()).append("</td>");
                    table.append("<td style='padding: 12px 20px; font-weight: bold; color: #2E7D32; border-right: 1px solid #eeeeee;'>").append(expenseDTO.getAmount()).append("</td>");

                    // Fallback for missing categories
                    String category = expenseDTO.getCategoryId() != null ? expenseDTO.getCategoryName() : "<span style='color: #999; font-style: italic;'>Uncategorized</span>";
                    table.append("<td style='padding: 12px 20px;'>").append(category).append("</td>"); // No right border on the last column
                    table.append("</tr>");
                }
                table.append("</table>");
                table.append("</div>");

                // Safely handle missing names and fix spacing
                String name = profile.getFullName();
                if (name == null || name.trim().isEmpty()) {
                    name = "User";
                }

                String body = "<div style='font-family: Arial, sans-serif; font-size: 15px; line-height: 1.6; color: #333; width: 100%;'>"
                        + "<h2 style='color: #4CAF50;'>Daily Expense Summary</h2>"
                        + "<p>Hi <b>" + name + "</b>,</p>"
                        + "<p>Here is a quick look at your tracked expenses for today:</p>"
                        + table.toString()
                        + "<br><p>Keep up the great work managing your finances!</p>"
                        + "<p>Best Regards,<br><b>ManageMe Team</b></p>"
                        + "</div>";

                emailService.sendEmail(profile.getEmail(), "Your Daily Expense Summary", body);
            }
        }
        log.info("Job finished: sendDailyExpenseSummary()");
    }
}

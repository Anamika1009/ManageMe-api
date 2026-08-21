package com.manage.manageme.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manage.manageme.dto.ChatDTOs.*;
import com.manage.manageme.dto.CategoryDTO;
import com.manage.manageme.dto.ExpenseDTO;
import com.manage.manageme.dto.IncomeDTO;
import com.manage.manageme.entity.ChatMessage;
import com.manage.manageme.entity.ChatThread;
import com.manage.manageme.entity.ProfileEntity;
import com.manage.manageme.repository.ChatMessageRepository;
import com.manage.manageme.repository.ChatThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    @Value("${groq.api.key}")
    private String apiKey;

    private final CategoryService categoryService;
    private final ExpenseService expenseService;
    private final IncomeService incomeService;
    private final ProfileService profileService;
    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;

    public List<ChatThread> getThreadsForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        return chatThreadRepository.findByProfileIdOrderByCreatedAtDesc(profile.getId());
    }

    public ChatThread createNewThread(String title) {
        ProfileEntity profile = profileService.getCurrentProfile();
        ChatThread thread = ChatThread.builder()
                .profileId(profile.getId())
                .title(title != null && !title.isEmpty() ? title : "New Chat " + LocalDate.now())
                .build();
        return chatThreadRepository.save(thread);
    }

    public List<ChatMessage> getMessagesForThread(Long threadId) {
        return chatMessageRepository.findByThreadIdOrderByCreatedAtAsc(threadId);
    }

    public ChatResponse processUserMessage(Long threadId, String userMessage) throws Exception {
        ProfileEntity profile = profileService.getCurrentProfile();
        
        // Ensure thread exists and belongs to user
        ChatThread thread = chatThreadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Thread not found"));

        // 1. Save incoming user message to DB
        ChatMessage userDbMsg = ChatMessage.builder()
                .threadId(threadId)
                .role("user")
                .content(userMessage)
                .build();
        chatMessageRepository.save(userDbMsg);

        // 2. Fetch context
        List<CategoryDTO> categories = categoryService.getCategoriesForCurrentProfile();
        String categoriesList = categories.stream()
            .map(category -> category.getName() + " (" + category.getType() + ")")
            .collect(Collectors.joining(", "));
        
        String totalExpense = expenseService.getTotalExpenseForCurrentUser().toPlainString();
        String totalIncome = incomeService.getTotalIncomeForCurrentUser().toPlainString();
        
        String systemPrompt = "You are a friendly financial assistant embedded in ManageMe, an income and expense tracking app.\n" +
                "The user's existing categories are: " + categoriesList + "\n" +
                "This month's total expenses: $" + totalExpense + "\n" +
                "This month's total income: $" + totalIncome + "\n" +
                "Today's date is: " + LocalDate.now() + "\n\n" +
                "RULES & BOUNDARIES:\n" +
                "1. If the user describes spending money, call the 'log_expense' function.\n" +
                "2. If the user describes receiving or earning money, call the 'log_income' function.\n" +
                "3. DATE PARSING: Extract transaction date if mentioned (e.g., 'yesterday', '2026-08-15'). Format as 'YYYY-MM-DD'. Default to today (" + LocalDate.now() + ").\n" +
                "4. Keep replies conversational and short (1-2 sentences).";

        // 3. Load thread history from DB
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        
        List<ChatMessage> threadHistory = chatMessageRepository.findByThreadIdOrderByCreatedAtAsc(threadId);
        for (ChatMessage msg : threadHistory) {
            messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }

        // 4. Define Tools
        Map<String, Object> logExpenseTool = Map.of(
            "type", "function",
            "function", Map.of(
                "name", "log_expense",
                "description", "Log a new expense",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "name", Map.of("type", "string", "description", "What was spent on"),
                        "amount", Map.of("type", "number", "description", "Amount spent"),
                        "category", Map.of("type", "string", "description", "Matching expense category"),
                        "date", Map.of("type", "string", "description", "Date in YYYY-MM-DD format")
                    ),
                    "required", List.of("name", "amount", "category")
                )
            )
        );

        Map<String, Object> logIncomeTool = Map.of(
            "type", "function",
            "function", Map.of(
                "name", "log_income",
                "description", "Log new income",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "name", Map.of("type", "string", "description", "Source of income"),
                        "amount", Map.of("type", "number", "description", "Amount received"),
                        "category", Map.of("type", "string", "description", "Matching income category"),
                        "date", Map.of("type", "string", "description", "Date in YYYY-MM-DD format")
                    ),
                    "required", List.of("name", "amount", "category")
                )
            )
        );

        Map<String, Object> requestBody = Map.of(
            "model", "qwen/qwen3.6-27b",
            "messages", messages,
            "tools", List.of(logExpenseTool, logIncomeTool),
            "tool_choice", "auto"
        );

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.groq.com/openai/v1/chat/completions",
                new HttpEntity<>(requestBody, headers),
                Map.class
        );

        Map<String, Object> responseBody = response.getBody();
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

        String assistantReply = "";
        ChatAction action = null;

        if (message.containsKey("tool_calls")) {
            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
            Map<String, Object> function = (Map<String, Object>) toolCalls.get(0).get("function");
            
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> args = mapper.readValue((String) function.get("arguments"), Map.class);

            LocalDate transactionDate = LocalDate.now();
            if (args.containsKey("date") && args.get("date") != null) {
                try {
                    transactionDate = LocalDate.parse(String.valueOf(args.get("date")));
                } catch (DateTimeParseException ignored) {}
            }

            if ("log_expense".equals(function.get("name"))) {
                String categoryName = String.valueOf(args.get("category"));
                CategoryDTO category = categories.stream()
                    .filter(item -> item.getName() != null && item.getName().equalsIgnoreCase(categoryName) && "expense".equalsIgnoreCase(item.getType()))
                    .findFirst()
                    .orElseGet(() -> categories.stream().filter(c -> "expense".equalsIgnoreCase(c.getType())).findFirst().orElse(null));

                ExpenseDTO expense = ExpenseDTO.builder()
                    .name(String.valueOf(args.get("name")))
                    .amount(new BigDecimal(String.valueOf(args.get("amount"))))
                    .categoryId(category != null ? category.getId() : null)
                    .categoryName(category != null ? category.getName() : "General")
                    .icon("💸")
                    .date(transactionDate)
                    .build();
                ExpenseDTO saved = expenseService.addExpense(expense);

                assistantReply = "Got it! Logged $" + saved.getAmount() + " for " + saved.getName() + " on " + saved.getDate() + ".";
                action = new ChatAction("EXPENSE_LOGGED", Map.of(
                    "id", saved.getId(), "name", saved.getName(), "amount", saved.getAmount(), "category", saved.getCategoryName(), "date", saved.getDate().toString()
                ));
            } else if ("log_income".equals(function.get("name"))) {
                String categoryName = String.valueOf(args.get("category"));
                CategoryDTO category = categories.stream()
                    .filter(item -> item.getName() != null && item.getName().equalsIgnoreCase(categoryName) && "income".equalsIgnoreCase(item.getType()))
                    .findFirst()
                    .orElseGet(() -> categories.stream().filter(c -> "income".equalsIgnoreCase(c.getType())).findFirst().orElse(null));

                IncomeDTO income = IncomeDTO.builder()
                    .name(String.valueOf(args.get("name")))
                    .amount(new BigDecimal(String.valueOf(args.get("amount"))))
                    .categoryId(category != null ? category.getId() : null)
                    .categoryName(category != null ? category.getName() : "General")
                    .icon("💰")
                    .date(transactionDate)
                    .build();
                IncomeDTO saved = incomeService.addIncome(income);

                assistantReply = "Got it! Logged $" + saved.getAmount() + " received for " + saved.getName() + " on " + saved.getDate() + ".";
                action = new ChatAction("INCOME_LOGGED", Map.of(
                    "id", saved.getId(), "name", saved.getName(), "amount", saved.getAmount(), "category", saved.getCategoryName(), "date", saved.getDate().toString()
                ));
            }
        } else {
            assistantReply = (String) message.get("content");
        }

        // 5. Save assistant reply to DB
        ChatMessage assistantDbMsg = ChatMessage.builder()
                .threadId(threadId)
                .role("assistant")
                .content(assistantReply)
                .build();
        chatMessageRepository.save(assistantDbMsg);

        return new ChatResponse(assistantReply, action);
    }
}
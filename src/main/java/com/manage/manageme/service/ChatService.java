package com.manage.manageme.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manage.manageme.dto.ChatDTOs.*;
import com.manage.manageme.dto.CategoryDTO;
import com.manage.manageme.dto.ExpenseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class ChatService {

    @Value("${groq.api.key}") // Using the same Groq key placeholder
    private String apiKey;

    private final CategoryService categoryService;
    private final ExpenseService expenseService;

    public ChatService(CategoryService categoryService, ExpenseService expenseService) {
        this.categoryService = categoryService;
        this.expenseService = expenseService;
    }
    
    public ChatResponse processUserMessage(String userMessage, List<Map<String, String>> history) throws Exception {
        
        // 1. Fetch context for the authenticated user
        List<CategoryDTO> categories = categoryService.getCategoriesForCurrentProfile();
        String categoriesList = categories.stream()
            .map(category -> category.getName() + " (" + category.getType() + ")")
            .reduce((left, right) -> left + ", " + right)
            .orElse("No categories available");
        String totalExpense = expenseService.getTotalExpenseForCurrentUser().toPlainString();
        
        // 2. Build System Prompt
        String systemPrompt = "You are a financial assistant embedded in an expense tracking app.\n" +
                "The user's existing categories are: " + categoriesList + "\n" +
                "This month's total expenses: $" + totalExpense + "\n" +
                "Today's date is: " + LocalDate.now() + "\n" +
                "Your job:\n" +
                "1. If the user describes spending money, call the 'log_expense' function.\n" +
                "2. If they ask about spending, answer using ONLY the data provided above.\n" +
                "3. Keep replies conversational and short (1-2 sentences).";

        // 3. Prepare Messages Array
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        
        if (history != null) {
            for (Map<String, String> msg : history) {
                messages.add(Map.of("role", msg.get("role"), "content", msg.get("content")));
            }
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        // 4. Define the Function (Tool)
        Map<String, Object> logExpenseTool = Map.of(
            "type", "function",
            "function", Map.of(
                "name", "log_expense",
                "description", "Log a new expense when the user describes spending money",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "name", Map.of("type", "string", "description", "What the money was spent on (e.g. Cab, Food)"),
                        "amount", Map.of("type", "number", "description", "Amount spent"),
                        "category", Map.of("type", "string", "description", "Best matching category from the user's list")
                    ),
                    "required", List.of("name", "amount", "category")
                )
            )
        );

        // 5. Build Groq Payload
        Map<String, Object> requestBody = Map.of(
            "model", "llama-3.3-70b-versatile", // Best Groq model for function calling
            "messages", messages,
            "tools", List.of(logExpenseTool),
            "tool_choice", "auto"
        );

        // 6. Call API
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim()); // Trimmed to prevent 500 errors!

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.groq.com/openai/v1/chat/completions",
                new HttpEntity<>(requestBody, headers),
                Map.class
        );

        // 7. Parse Response
        Map<String, Object> responseBody = response.getBody();
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

        // CHECK IF GROQ CALLED A FUNCTION
        if (message.containsKey("tool_calls")) {
            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
            Map<String, Object> function = (Map<String, Object>) toolCalls.get(0).get("function");
            
            if ("log_expense".equals(function.get("name"))) {
                String argsJson = (String) function.get("arguments");
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> args = mapper.readValue(argsJson, Map.class);

                String categoryName = String.valueOf(args.get("category"));
                CategoryDTO category = categories.stream()
                    .filter(item -> item.getName() != null
                        && item.getName().equalsIgnoreCase(categoryName)
                        && "expense".equalsIgnoreCase(item.getType()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                        "No matching expense category found for: " + categoryName));

                ExpenseDTO expense = ExpenseDTO.builder()
                    .name(String.valueOf(args.get("name")))
                    .amount(new BigDecimal(String.valueOf(args.get("amount"))))
                    .categoryId(category.getId())
                    .categoryName(category.getName())
                    .icon("💸")
                    .date(LocalDate.now())
                    .build();
                ExpenseDTO savedExpense = expenseService.addExpense(expense);

                return new ChatResponse(
                    "Got it! I've logged $" + savedExpense.getAmount() + " for " + savedExpense.getName() + ".",
                    new ChatAction("EXPENSE_LOGGED", Map.of(
                        "id", savedExpense.getId(),
                        "name", savedExpense.getName(),
                        "amount", savedExpense.getAmount(),
                        "category", savedExpense.getCategoryName(),
                        "date", savedExpense.getDate()
                    ))
                );
            }
        }

        // NO FUNCTION CALLED -> Just return the plain text reply
        return new ChatResponse((String) message.get("content"), null);
    }
}
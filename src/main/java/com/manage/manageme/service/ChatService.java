package com.manage.manageme.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manage.manageme.dto.ChatDTOs.*;
import com.manage.manageme.dto.CategoryDTO;
import com.manage.manageme.dto.ExpenseDTO;
import com.manage.manageme.dto.IncomeDTO;
import com.manage.manageme.entity.ChatMessage;
import com.manage.manageme.entity.Profile;
import com.manage.manageme.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Value("${groq.api.key}")
    private String apiKey;

    private final CategoryService categoryService;
    private final ExpenseService expenseService;
    private final IncomeService incomeService;
    private final ChatMessageRepository chatMessageRepository;
    private final ProfileService profileService;

    public ChatService(CategoryService categoryService, ExpenseService expenseService, 
                       IncomeService incomeService, ChatMessageRepository chatMessageRepository, 
                       ProfileService profileService) {
        this.categoryService = categoryService;
        this.expenseService = expenseService;
        this.incomeService = incomeService;
        this.chatMessageRepository = chatMessageRepository;
        this.profileService = profileService;
    }

    public List<ChatMessage> getChatHistoryForCurrentUser() {
        Profile profile = profileService.getCurrentProfile();
        return chatMessageRepository.findByProfileIdOrderByCreatedAtAsc(profile.getId());
    }
    
    public ChatResponse processUserMessage(String userMessage, List<Map<String, String>> history) throws Exception {
        Profile profile = profileService.getCurrentProfile();
        Long profileId = profile.getId();

        // 1. Save incoming User message to DB for persistence
        ChatMessage userDbMsg = ChatMessage.builder()
                .profileId(profileId)
                .role("user")
                .content(userMessage)
                .build();
        chatMessageRepository.save(userDbMsg);

        // 2. Fetch context for the authenticated user
        List<CategoryDTO> categories = categoryService.getCategoriesForCurrentProfile();
        String categoriesList = categories.stream()
            .map(category -> category.getName() + " (" + category.getType() + ")")
            .collect(Collectors.joining(", "));
        
        String totalExpense = expenseService.getTotalExpenseForCurrentUser().toPlainString();
        String totalIncome = incomeService.getTotalIncomeForCurrentUser().toPlainString();
        
        // 3. Build System Prompt with STRICT Financial Boundaries
        String systemPrompt = "You are a friendly financial assistant embedded in ManageMe, an income and expense tracking app.\n" +
                "The user's existing categories are: " + categoriesList + "\n" +
                "This month's total expenses: $" + totalExpense + "\n" +
                "This month's total income: $" + totalIncome + "\n" +
                "Today's date is: " + LocalDate.now() + "\n\n" +
                "RULES & BOUNDARIES:\n" +
                "1. If the user describes spending money, call the 'log_expense' function. Match category precisely.\n" +
                "2. If the user describes receiving or earning money, call the 'log_income' function.\n" +
                "3. If they ask about spending or income, answer using ONLY the data provided above. Do not hallucinate numbers.\n" +
                "4. STRICT FINANCIAL BOUNDARY: Never give specific investment, stock, crypto, or tax advice. Keep guidance practical, non-judgmental, brief, and general.\n" +
                "5. Keep replies conversational and short (1-2 sentences).";

        // 4. Prepare Messages Array (Pull full history from DB for continuity)
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        
        List<ChatMessage> savedHistory = chatMessageRepository.findByProfileIdOrderByCreatedAtAsc(profileId);
        for (ChatMessage msg : savedHistory) {
            messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }

        // 5. Define Tools (Functions)
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
                        "category", Map.of("type", "string", "description", "Best matching expense category from the user's list")
                    ),
                    "required", List.of("name", "amount", "category")
                )
            )
        );

        Map<String, Object> logIncomeTool = Map.of(
            "type", "function",
            "function", Map.of(
                "name", "log_income",
                "description", "Log money received or earned when the user describes income",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "name", Map.of("type", "string", "description", "What the money was received for (e.g. Salary, Freelance)"),
                        "amount", Map.of("type", "number", "description", "Amount received"),
                        "category", Map.of("type", "string", "description", "Best matching income category from the user's list")
                    ),
                    "required", List.of("name", "amount", "category")
                )
            )
        );

        // 6. Build Groq Payload
        Map<String, Object> requestBody = Map.of(
            "model", "qwen/qwen3.6-27b",
            "messages", messages,
            "tools", List.of(logExpenseTool, logIncomeTool),
            "tool_choice", "auto"
        );

        // 7. Call API
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.groq.com/openai/v1/chat/completions",
                new HttpEntity<>(requestBody, headers),
                Map.class
        );

        // 8. Parse Response
        Map<String, Object> responseBody = response.getBody();
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

        String assistantReply = "";
        ChatAction action = null;

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
                    .orElseGet(() -> categories.stream().filter(c -> "expense".equalsIgnoreCase(c.getType())).findFirst().orElse(null));

                if (category == null) {
                    assistantReply = "I couldn't match '" + categoryName + "' to an expense category. Which category should I use?";
                } else {
                    ExpenseDTO expense = ExpenseDTO.builder()
                        .name(String.valueOf(args.get("name")))
                        .amount(new BigDecimal(String.valueOf(args.get("amount"))))
                        .categoryId(category.getId())
                        .categoryName(category.getName())
                        .icon("💸")
                        .date(LocalDate.now())
                        .build();
                    ExpenseDTO savedExpense = expenseService.addExpense(expense);

                    assistantReply = "Got it! I've logged $" + savedExpense.getAmount() + " for " + savedExpense.getName() + ".";
                    action = new ChatAction("EXPENSE_LOGGED", Map.of(
                        "id", savedExpense.getId(),
                        "name", savedExpense.getName(),
                        "amount", savedExpense.getAmount(),
                        "category", savedExpense.getCategoryName(),
                        "date", savedExpense.getDate()
                    ));
                }
            } else if ("log_income".equals(function.get("name"))) {
                String argsJson = (String) function.get("arguments");
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> args = mapper.readValue(argsJson, Map.class);

                String categoryName = String.valueOf(args.get("category"));
                CategoryDTO category = categories.stream()
                    .filter(item -> item.getName() != null
                        && item.getName().equalsIgnoreCase(categoryName)
                        && "income".equalsIgnoreCase(item.getType()))
                    .findFirst()
                    .orElseGet(() -> categories.stream().filter(c -> "income".equalsIgnoreCase(c.getType())).findFirst().orElse(null));

                if (category == null) {
                    assistantReply = "I couldn't match '" + categoryName + "' to an income category. Which category should I use?";
                } else {
                    IncomeDTO income = IncomeDTO.builder()
                        .name(String.valueOf(args.get("name")))
                        .amount(new BigDecimal(String.valueOf(args.get("amount"))))
                        .categoryId(category.getId())
                        .categoryName(category.getName())
                        .icon("💰")
                        .date(LocalDate.now())
                        .build();
                    IncomeDTO savedIncome = incomeService.addIncome(income);

                    assistantReply = "Got it! I've logged $" + savedIncome.getAmount() + " received for " + savedIncome.getName() + ".";
                    action = new ChatAction("INCOME_LOGGED", Map.of(
                        "id", savedIncome.getId(),
                        "name", savedIncome.getName(),
                        "amount", savedIncome.getAmount(),
                        "category", savedIncome.getCategoryName(),
                        "date", savedIncome.getDate()
                    ));
                }
            }
        } else {
            assistantReply = (String) message.get("content");
        }

        // 9. Save Assistant response to DB for history continuity
        ChatMessage assistantDbMsg = ChatMessage.builder()
                .profileId(profileId)
                .role("assistant")
                .content(assistantReply)
                .build();
        chatMessageRepository.save(assistantDbMsg);

        return new ChatResponse(assistantReply, action);
    }
}
package com.manage.manageme.controller;

import com.manage.manageme.dto.ExpenseDTO;
import com.manage.manageme.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/expenses")
public class ExpenseController {
    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<?> addExpense(@RequestBody ExpenseDTO expenseDTO) {
        try {
            ExpenseDTO saved = expenseService.addExpense(expenseDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            String message = (e.getMessage() != null) ? e.getMessage() : "Failed to add expense.";
            return ResponseEntity.badRequest().body(Map.of("message", message));
        }
    }

    // Returns ALL expenses for the current user; frontend filters by month/year/search
    @GetMapping
    public ResponseEntity<List<ExpenseDTO>> getExpenses(){
        List<ExpenseDTO> expenses = expenseService.getAllExpensesForCurrentUser();
        return ResponseEntity.status(HttpStatus.OK).body(expenses);
    }

    // Controller for delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id){
        expenseService.deleteExpense(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
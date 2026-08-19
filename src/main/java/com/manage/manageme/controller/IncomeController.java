package com.manage.manageme.controller;

import com.manage.manageme.dto.IncomeDTO;
import com.manage.manageme.service.IncomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/incomes")
public class IncomeController {
    private final IncomeService incomeService;

    @PostMapping
    public ResponseEntity<?> addExpense(@RequestBody IncomeDTO incomeDTO) {
        try {
            IncomeDTO saved = incomeService.addIncome(incomeDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            String message = (e.getMessage() != null) ? e.getMessage() : "Failed to add income.";
            return ResponseEntity.badRequest().body(Map.of("message", message));
        }
    }

    // Returns ALL incomes for the current user; frontend filters by month/year/search
    @GetMapping
    public ResponseEntity<List<IncomeDTO>> getIncomes(){
        List<IncomeDTO> incomes = incomeService.getAllIncomesForCurrentUser();
        return ResponseEntity.status(HttpStatus.OK).body(incomes);
    }

    // Controller for delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncome(@PathVariable Long id){
        incomeService.deleteIncome(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
package com.manage.manageme.service;

import com.manage.manageme.dto.ExpenseDTO;
import com.manage.manageme.entity.CategoryEntity;
import com.manage.manageme.entity.ExpenseEntity;
import com.manage.manageme.entity.ProfileEntity;
import com.manage.manageme.repository.CategoryRepository;
import com.manage.manageme.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final CategoryService categoryService;
    private final CategoryRepository categoryRepository;
    private final ProfileService profileService;

    @Transactional
    public ExpenseDTO addExpense(ExpenseDTO expenseDTO) {
        ProfileEntity profile = profileService.getCurrentProfile();
        CategoryEntity category = categoryRepository.findById(expenseDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        ExpenseEntity newExpense = toEntity(expenseDTO, profile, category);
        newExpense = expenseRepository.save(newExpense);
        return toDTO(newExpense);
    }

    // Retrieve ALL expenses for the current user (no month restriction —
    // frontend's Quick Timeline Filter handles narrowing by month/year)
    public List<ExpenseDTO> getAllExpensesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<ExpenseEntity> expenses = expenseRepository.findByProfileIdOrderByDateDesc(profile.getId());
        return expenses.stream().map(this::toDTO).toList();
    }

    // Retrieve all the expenses for the current month based on the start date and end date
    // (kept for other callers, e.g. report/email generation)
    public List<ExpenseDTO> getCurrentMonthExpensesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        List<ExpenseEntity> expenses = expenseRepository.findByProfileIdAndDateBetween(profile.getId(), startOfMonth, endOfMonth);
        return expenses.stream().map(this::toDTO).toList();
    }

    // Delete the expenses by id for the current user
    @Transactional
    public void deleteExpense(Long expenseId){
        ProfileEntity profile = profileService.getCurrentProfile();
        ExpenseEntity entity = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        if(!entity.getProfile().getId().equals(profile.getId())){
            throw new RuntimeException("unauthorized to delete this expense");
        }
        expenseRepository.delete(entity);
    }

    // Get latest 5 expenses for the current user
    public List<ExpenseDTO> getLatest5ExpensesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<ExpenseEntity> expenses = expenseRepository.findTop5ByProfileIdOrderByDateDesc(profile.getId());
        return expenses.stream().map(this::toDTO).toList();
    }

    // Get total expenses for current user
    public BigDecimal getTotalExpenseForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        BigDecimal total = expenseRepository.findTotalExpensesByProfileId(profile.getId());
        return total != null ? total : BigDecimal.ZERO;
    }

    // filter expenses by category for the current month
    public List<ExpenseDTO> filterExpenses(LocalDate startDate, LocalDate endDate, String keyword, Sort sort) {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<ExpenseEntity> expenses = expenseRepository.findByProfileIdAndDateBetweenAndNameContainingIgnoreCase(profile.getId(), startDate, endDate, keyword, sort);
        return expenses.stream().map(this::toDTO).toList();
    }

    // Notification
    public List<ExpenseDTO> getExpensesForUserOnDate(Long profileId , LocalDate date) {
        List<ExpenseEntity> expenses = expenseRepository.findByProfileIdAndDate(profileId, date);
        return expenses.stream().map(this::toDTO).toList();
    }

    // helper method
    private ExpenseEntity toEntity (ExpenseDTO expenseDTO, ProfileEntity profile, CategoryEntity category) {
        return ExpenseEntity.builder()
                .name(expenseDTO.getName())
                .icon(expenseDTO.getIcon())
                .amount(expenseDTO.getAmount())
                .date(expenseDTO.getDate())
                .profile(profile)
                .category(category)
                .build();
    }

    private ExpenseDTO toDTO (ExpenseEntity expenseEntity) {
        return ExpenseDTO.builder()
                .id(expenseEntity.getId())
                .name(expenseEntity.getName())
                .icon(expenseEntity.getIcon())
                .amount(expenseEntity.getAmount())
                .date(expenseEntity.getDate())
                .createdAt(expenseEntity.getCreatedAt())
                .updatedAt(expenseEntity.getUpdatedAt())
                .categoryId(expenseEntity.getCategory() != null ? expenseEntity.getCategory().getId() : null)
                .categoryName(expenseEntity.getCategory() != null ? expenseEntity.getCategory().getName() : "N/A")
                .build();
    }
}
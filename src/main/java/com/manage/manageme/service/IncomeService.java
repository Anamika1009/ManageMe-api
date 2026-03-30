package com.manage.manageme.service;
import com.manage.manageme.dto.ExpenseDTO;
import com.manage.manageme.dto.IncomeDTO;
import com.manage.manageme.entity.CategoryEntity;
import com.manage.manageme.entity.ExpenseEntity;
import com.manage.manageme.entity.IncomeEntity;
import com.manage.manageme.entity.ProfileEntity;
import com.manage.manageme.repository.CategoryRepository;
import com.manage.manageme.repository.ExpenseRepository;
import com.manage.manageme.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncomeService {
    private final IncomeRepository incomeRepository;
    private final CategoryRepository categoryRepository;
    private final ProfileService profileService;
    private final CategoryService categoryService;

    public IncomeDTO addIncome(IncomeDTO incomeDTO) {
        ProfileEntity profile = profileService.getCurrentProfile();
        CategoryEntity category = categoryRepository.findById(incomeDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        IncomeEntity newIncome = toEntity(incomeDTO, profile, category);
        newIncome    = incomeRepository.save(newIncome);
        return toDTO(newIncome);
    }

    // Retrieve all the incomes for the current month based on the start date and end date
    public List<IncomeDTO> getCurrentMonthIncomesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        List<IncomeEntity> incomes =incomeRepository.findByProfileIdAndDateBetween(profile.getId(), startOfMonth, endOfMonth);
        return incomes.stream().map(this::toDTO).toList();
    }


    // Delete the income by id for the current user
    public void deleteIncome(Long incomeId){
        ProfileEntity profile = profileService.getCurrentProfile();
        IncomeEntity entity = incomeRepository.findById(incomeId)
                .orElseThrow(() -> new RuntimeException("Income not found"));
        if(!entity.getProfile().getId().equals(profile.getId())){
            throw new RuntimeException("unauthorized to delete this income");
        }
        incomeRepository.delete(entity);
    }


    // Get latest 5 incomes for the current user
    public List<IncomeDTO> getLatest5IncomeForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<IncomeEntity> income = incomeRepository.findTop5ByProfileIdOrderByDateDesc(profile.getId());
        return income.stream().map(this::toDTO).toList();
    }

    // Get total incomes for current user
    public BigDecimal getTotalIncomeForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        BigDecimal total = incomeRepository.findTotalIncomeByProfileId(profile.getId());
        return total != null? total : BigDecimal.ZERO;
    }

    // filter incomes by category for the current month
    public List<IncomeDTO> filterIncomes(LocalDate startDate, LocalDate endDate, String keyword, Sort sort) {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<IncomeEntity> incomes = incomeRepository.findByProfileIdAndDateBetweenAndNameContainingIgnoreCase(profile.getId(), startDate, endDate, keyword, sort);
        return incomes.stream().map(this::toDTO).toList();
    }


    // helper method
    // this method is used to convert the IncomeDTO to IncomeEntity and set the profile and category
    private IncomeEntity toEntity  (IncomeDTO incomeDTO, ProfileEntity profile, CategoryEntity category) {
        return IncomeEntity.builder()
                .name(incomeDTO.getName())
                .icon(incomeDTO.getIcon())
                .amount(incomeDTO.getAmount())
                .date(incomeDTO.getDate())
                .profile(profile)
                .category(category)
                .build();
    }

    private IncomeDTO toDTO (IncomeEntity incomeEntity) {
        return IncomeDTO.builder()
                .id(incomeEntity.getId())
                .name(incomeEntity.getName())
                .icon(incomeEntity.getIcon())
                .amount(incomeEntity.getAmount())
                .date(incomeEntity.getDate())
                .createdAt(incomeEntity.getCreatedAt())
                .updatedAt(incomeEntity.getUpdatedAt())
                .categoryId(incomeEntity.getCategory() != null ? incomeEntity.getCategory().getId()
                        : null)
                .categoryName(incomeEntity.getCategory() != null ? incomeEntity.getCategory().getName()
                        : "N/A")
                .build();


    }
}

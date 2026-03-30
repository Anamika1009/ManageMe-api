package com.manage.manageme.repository;

import com.manage.manageme.entity.ExpenseEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<ExpenseEntity, Long> {

    // SQL Query : Select * from income where profile_id = ? order by date desc
    List<ExpenseEntity> findByProfileIdOrderByDateDesc(Long profileId);

    // Find by method to find the top 5 expenses of the current logged-in user
    // SQL Query : Select * from income where profile_id = ? order by date desc in limit of 5
    List<ExpenseEntity> findTop5ByProfileIdOrderByDateDesc(Long profileId);

    // find by the total expenses of the current logged-in user
    @Query("SELECT SUM(e.amount) FROM ExpenseEntity e WHERE e.profile.id = :profileId")
    BigDecimal findTotalExpensesByProfileId(@Param("profileId") Long profileId);

    List <ExpenseEntity> findByProfileIdAndDateBetweenAndNameContainingIgnoreCase(
            Long profileId,
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            Sort sort
    );

    List<ExpenseEntity> findByProfileIdAndDateBetween(Long profileId, LocalDate startDate, LocalDate endDate);

    List<ExpenseEntity> findTop5ByProfileIdOrderByDateDesc(Long profileId, Limit limit);

    List<ExpenseEntity> findByProfileIdAndDate(Long profileId, LocalDate date);
}



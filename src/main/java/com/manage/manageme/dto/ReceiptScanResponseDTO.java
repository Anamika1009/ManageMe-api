package com.manage.manageme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptScanResponseDTO {
    private String merchantName;
    private BigDecimal amount;
    private String date;           // ISO format: YYYY-MM-DD
    private String suggestedCategory;
    private String icon;
}
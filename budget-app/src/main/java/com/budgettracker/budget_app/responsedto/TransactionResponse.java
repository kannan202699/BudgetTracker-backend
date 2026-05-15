package com.budgettracker.budget_app.responsedto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private Long id;
    private String type;
    private Double amount;
    private String category;
    private String description;
    private LocalDate date;
    private String username;
}

package com.budgettracker.budget_app.responsedto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RecurringTransactionResponse {
    private Long id;
    private String type;
    private Double amount;
    private String category;
    private String description;
    private String frequency;
    private Integer dayOfMonth;
    private Boolean active;
}

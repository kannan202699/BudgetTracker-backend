package com.budgettracker.budget_app.responsedto;

import lombok.*;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SavingsGoalResponse {
    private Long id;
    private String title;
    private Double targetAmount;
    private Double savedAmount;
    private LocalDate deadline;
}

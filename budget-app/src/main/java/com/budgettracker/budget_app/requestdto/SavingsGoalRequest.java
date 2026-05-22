package com.budgettracker.budget_app.requestdto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SavingsGoalRequest {
    @NotBlank @Size(max = 100)
    private String title;
    @NotNull @DecimalMin("1.00") @DecimalMax("99999999.00")
    private Double targetAmount;
    @NotNull @DecimalMin("0.00") @DecimalMax("99999999.00")
    private Double savedAmount;
    private LocalDate deadline;
}

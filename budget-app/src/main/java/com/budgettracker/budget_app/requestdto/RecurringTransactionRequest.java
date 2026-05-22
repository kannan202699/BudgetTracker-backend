package com.budgettracker.budget_app.requestdto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RecurringTransactionRequest {
    @NotBlank @Pattern(regexp = "^(INCOME|EXPENSE)$")
    private String type;
    @NotNull @DecimalMin("0.01") @DecimalMax("99999999.00")
    private Double amount;
    @NotBlank @Size(max = 50)
    private String category;
    @Size(max = 200)
    private String description;
    @NotBlank @Pattern(regexp = "^(MONTHLY|WEEKLY)$")
    private String frequency;
    @NotNull @Min(1) @Max(31)
    private Integer dayOfMonth;
    private Boolean active;
}

package com.budgettracker.budget_app.requestdto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmiLoanRequest {
    @NotBlank
    @Size(max = 100)
    private String loanName;
    @NotNull
    @DecimalMin("1.00")
    @DecimalMax("999999999.00")
    private Double principal;
    @NotNull
    @DecimalMin("0.01")
    @DecimalMax("50.00")
    private Double interestRate;
    @NotNull
    @Min(1)
    @Max(600)
    private Integer tenureMonths;
    @NotNull
    private LocalDate startDate;
}

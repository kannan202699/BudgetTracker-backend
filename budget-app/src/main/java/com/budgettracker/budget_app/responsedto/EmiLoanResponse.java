package com.budgettracker.budget_app.responsedto;

import lombok.*;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmiLoanResponse {
    private Long id;
    private String loanName;
    private Double principal;
    private Double interestRate;
    private Integer tenureMonths;
    private LocalDate startDate;
    private Double emiAmount;
    private Double totalAmount;
    private Double totalInterest;
    private Long monthsElapsed;
    private Long monthsRemaining;
    private Double remainingBalance;
    private LocalDate nextDueDate;
    private Integer paidMonths;
}

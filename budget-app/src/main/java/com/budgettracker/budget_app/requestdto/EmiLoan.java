package com.budgettracker.budget_app.requestdto;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "emi_loans")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmiLoan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserRequest user;

    @NotBlank @Size(max = 100)
    private String loanName;

    @NotNull @DecimalMin("1.00") @DecimalMax("999999999.00")
    private Double principal;

    @NotNull @DecimalMin("0.01") @DecimalMax("50.00")
    private Double interestRate;

    @NotNull @Min(1) @Max(600)
    private Integer tenureMonths;

    @NotNull
    private LocalDate startDate;

    @Column(name = "paid_months", nullable = false, columnDefinition = "integer not null default 0")
    @Builder.Default
    private Integer paidMonths = 0;
}

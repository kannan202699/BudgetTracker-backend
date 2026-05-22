package com.budgettracker.budget_app.requestdto;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "recurring_transactions")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RecurringTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserRequest user;

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

    @Column(nullable = false)
    private Boolean active = true;
}

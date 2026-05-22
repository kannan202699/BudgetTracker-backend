package com.budgettracker.budget_app.requestdto;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "savings_goals")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SavingsGoal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserRequest user;

    @NotBlank @Size(max = 100)
    private String title;

    @NotNull @DecimalMin("1.00") @DecimalMax("99999999.00")
    private Double targetAmount;

    @NotNull @DecimalMin("0.00") @DecimalMax("99999999.00")
    private Double savedAmount;

    private LocalDate deadline;
}

package com.budgettracker.budget_app.requestdto;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "budget_goals", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "category"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserRequest user;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Double amount;
}

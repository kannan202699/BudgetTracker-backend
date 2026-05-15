package com.budgettracker.budget_app.requestdto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetGoalRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Budget must be at least ₹1")
    @DecimalMax(value = "99999999.00", message = "Budget exceeds the maximum allowed value")
    private Double amount;
}

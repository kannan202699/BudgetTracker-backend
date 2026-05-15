package com.budgettracker.budget_app.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ApiErrorResponse {

    private int status;
    private String error;
    private String path;
    private String message;
    private LocalDateTime timestamp;

}
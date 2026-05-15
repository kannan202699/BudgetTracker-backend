package com.budgettracker.budget_app.util;

import com.budgettracker.budget_app.requestdto.TransactionRequest;
import com.budgettracker.budget_app.responsedto.TransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class ResponseMapper {

    public static TransactionResponse toTransactionResponse(TransactionRequest t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .type(t.getType())
                .amount(t.getAmount())
                .category(t.getCategory())
                .description(t.getDescription())
                .date(t.getDate())
                .username(t.getUser() != null ? t.getUser().getUsername() : null)
                .build();
    }
}

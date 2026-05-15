package com.budgettracker.budget_app.util;

import com.budgettracker.budget_app.requestdto.TransactionRequest;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.responsedto.TransactionResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseMapperTest {

    @Test
    void toTransactionResponse_mapsAllFields() {
        UserRequest user = new UserRequest();
        user.setUsername("alice");

        TransactionRequest txn = new TransactionRequest();
        txn.setId(1L);
        txn.setType("INCOME");
        txn.setAmount(500.0);
        txn.setCategory("Salary");
        txn.setDescription("Monthly salary");
        txn.setDate(LocalDate.of(2024, 1, 15));
        txn.setUser(user);

        TransactionResponse response = ResponseMapper.toTransactionResponse(txn);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getType()).isEqualTo("INCOME");
        assertThat(response.getAmount()).isEqualTo(500.0);
        assertThat(response.getCategory()).isEqualTo("Salary");
        assertThat(response.getDescription()).isEqualTo("Monthly salary");
        assertThat(response.getDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(response.getUsername()).isEqualTo("alice");
    }

    @Test
    void toTransactionResponse_nullUser_usernameIsNull() {
        TransactionRequest txn = new TransactionRequest();
        txn.setId(2L);
        txn.setType("EXPENSE");
        txn.setAmount(100.0);
        txn.setCategory("Food");
        txn.setDate(LocalDate.now());
        txn.setUser(null);

        TransactionResponse response = ResponseMapper.toTransactionResponse(txn);

        assertThat(response.getUsername()).isNull();
    }
}

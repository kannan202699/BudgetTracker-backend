package com.budgettracker.budget_app.service;

import com.budgettracker.budget_app.exception.ForbiddenException;
import com.budgettracker.budget_app.exception.ResourceNotFoundException;
import com.budgettracker.budget_app.repository.RecurringTransactionRepository;
import com.budgettracker.budget_app.repository.UserRepository;
import com.budgettracker.budget_app.requestdto.RecurringTransaction;
import com.budgettracker.budget_app.requestdto.RecurringTransactionRequest;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.responsedto.RecurringTransactionResponse;
import com.budgettracker.budget_app.util.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecurringTransactionServiceTest {

    @Mock private RecurringTransactionRepository repo;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private RecurringTransactionService recurringTransactionService;

    private UserRequest user;
    private UserRequest otherUser;

    @BeforeEach
    void setUp() {
        user = new UserRequest();
        user.setId(1L);
        user.setUsername("alice");
        user.setRole(Role.USER);

        otherUser = new UserRequest();
        otherUser.setId(2L);
        otherUser.setUsername("bob");
        otherUser.setRole(Role.USER);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null, List.of())
        );
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private RecurringTransaction buildRt(Long id, UserRequest owner) {
        return RecurringTransaction.builder()
                .id(id).user(owner).type("EXPENSE").amount(500.0)
                .category("Groceries").frequency("MONTHLY").dayOfMonth(5).active(true).build();
    }

    private RecurringTransactionRequest buildRequest() {
        return RecurringTransactionRequest.builder()
                .type("EXPENSE").amount(500.0).category("Groceries")
                .frequency("MONTHLY").dayOfMonth(5).active(true).build();
    }

    // --- getAll ---

    @Test
    void getAll_returnsMappedList() {
        when(repo.findByUser(user)).thenReturn(List.of(buildRt(1L, user)));

        List<RecurringTransactionResponse> result = recurringTransactionService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("Groceries");
    }

    @Test
    void getAll_whenEmpty_returnsEmptyList() {
        when(repo.findByUser(user)).thenReturn(List.of());

        assertThat(recurringTransactionService.getAll()).isEmpty();
    }

    // --- create ---

    @Test
    void create_savesAndReturnsResponse() {
        when(repo.save(any(RecurringTransaction.class))).thenReturn(buildRt(10L, user));

        RecurringTransactionResponse resp = recurringTransactionService.create(buildRequest());

        assertThat(resp).isNotNull();
        assertThat(resp.getCategory()).isEqualTo("Groceries");
        verify(repo).save(any(RecurringTransaction.class));
    }

    // --- update ---

    @Test
    void update_ownRecord_updatesAndReturns() {
        RecurringTransaction existing = buildRt(1L, user);
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(existing)).thenReturn(existing);

        RecurringTransactionRequest req = buildRequest();
        req.setAmount(750.0);
        req.setCategory("Utilities");

        RecurringTransactionResponse resp = recurringTransactionService.update(1L, req);

        assertThat(resp.getAmount()).isEqualTo(750.0);
        assertThat(resp.getCategory()).isEqualTo("Utilities");
        verify(repo).save(existing);
    }

    @Test
    void update_otherUsersRecord_throwsForbidden() {
        when(repo.findById(1L)).thenReturn(Optional.of(buildRt(1L, otherUser)));

        assertThatThrownBy(() -> recurringTransactionService.update(1L, buildRequest()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void update_notFound_throwsResourceNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recurringTransactionService.update(99L, buildRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- delete ---

    @Test
    void delete_ownRecord_deletesSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(buildRt(1L, user)));

        recurringTransactionService.delete(1L);

        verify(repo).deleteById(1L);
    }

    @Test
    void delete_otherUsersRecord_throwsForbidden() {
        when(repo.findById(1L)).thenReturn(Optional.of(buildRt(1L, otherUser)));

        assertThatThrownBy(() -> recurringTransactionService.delete(1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void delete_notFound_throwsResourceNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recurringTransactionService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

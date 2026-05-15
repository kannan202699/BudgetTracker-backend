package com.budgettracker.budget_app.service;

import com.budgettracker.budget_app.repository.BudgetGoalRepository;
import com.budgettracker.budget_app.repository.UserRepository;
import com.budgettracker.budget_app.requestdto.BudgetGoal;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.util.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetGoalServiceTest {

    @Mock private BudgetGoalRepository budgetGoalRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private BudgetGoalService budgetGoalService;

    private UserRequest user;

    @BeforeEach
    void setUp() {
        user = new UserRequest();
        user.setId(1L);
        user.setUsername("alice");
        user.setRole(Role.USER);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null, List.of())
        );
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getGoals_returnsMapOfCategoryToAmount() {
        BudgetGoal g1 = BudgetGoal.builder().user(user).category("Food").amount(3000.0).build();
        BudgetGoal g2 = BudgetGoal.builder().user(user).category("Transport").amount(1500.0).build();
        when(budgetGoalRepository.findByUser(user)).thenReturn(List.of(g1, g2));

        Map<String, Double> result = budgetGoalService.getGoals();

        assertThat(result).hasSize(2)
                .containsEntry("Food", 3000.0)
                .containsEntry("Transport", 1500.0);
    }

    @Test
    void getGoals_whenNoGoals_returnsEmptyMap() {
        when(budgetGoalRepository.findByUser(user)).thenReturn(List.of());

        Map<String, Double> result = budgetGoalService.getGoals();

        assertThat(result).isEmpty();
    }

    @Test
    void setGoal_whenNoExistingGoal_createsNew() {
        when(budgetGoalRepository.findByUserAndCategory(user, "Food")).thenReturn(Optional.empty());
        when(budgetGoalRepository.save(any(BudgetGoal.class))).thenAnswer(inv -> inv.getArgument(0));

        budgetGoalService.setGoal("Food", 2500.0);

        ArgumentCaptor<BudgetGoal> captor = ArgumentCaptor.forClass(BudgetGoal.class);
        verify(budgetGoalRepository).save(captor.capture());
        BudgetGoal saved = captor.getValue();
        assertThat(saved.getCategory()).isEqualTo("Food");
        assertThat(saved.getAmount()).isEqualTo(2500.0);
        assertThat(saved.getUser()).isEqualTo(user);
    }

    @Test
    void setGoal_whenExistingGoal_updatesAmount() {
        BudgetGoal existing = BudgetGoal.builder().user(user).category("Food").amount(1000.0).build();
        when(budgetGoalRepository.findByUserAndCategory(user, "Food")).thenReturn(Optional.of(existing));
        when(budgetGoalRepository.save(any(BudgetGoal.class))).thenAnswer(inv -> inv.getArgument(0));

        budgetGoalService.setGoal("Food", 4000.0);

        ArgumentCaptor<BudgetGoal> captor = ArgumentCaptor.forClass(BudgetGoal.class);
        verify(budgetGoalRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualTo(4000.0);
    }

    @Test
    void deleteGoal_callsRepositoryDelete() {
        budgetGoalService.deleteGoal("Transport");

        verify(budgetGoalRepository).deleteByUserAndCategory(user, "Transport");
    }

    @Test
    void setGoal_verifiesUserLookup() {
        when(budgetGoalRepository.findByUserAndCategory(user, "Health")).thenReturn(Optional.empty());
        when(budgetGoalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        budgetGoalService.setGoal("Health", 500.0);

        verify(userRepository).findByUsername("alice");
    }
}

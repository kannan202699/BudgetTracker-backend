package com.budgettracker.budget_app.service;

import com.budgettracker.budget_app.exception.ForbiddenException;
import com.budgettracker.budget_app.exception.ResourceNotFoundException;
import com.budgettracker.budget_app.repository.SavingsGoalRepository;
import com.budgettracker.budget_app.repository.UserRepository;
import com.budgettracker.budget_app.requestdto.SavingsGoal;
import com.budgettracker.budget_app.requestdto.SavingsGoalRequest;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.responsedto.SavingsGoalResponse;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SavingsGoalServiceTest {

    @Mock private SavingsGoalRepository repo;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private SavingsGoalService savingsGoalService;

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

    private SavingsGoal buildGoal(Long id, UserRequest owner) {
        return SavingsGoal.builder()
                .id(id).user(owner).title("Vacation Fund")
                .targetAmount(10000.0).savedAmount(2500.0)
                .deadline(LocalDate.now().plusYears(1)).build();
    }

    private SavingsGoalRequest buildRequest() {
        return SavingsGoalRequest.builder()
                .title("Vacation Fund").targetAmount(10000.0)
                .savedAmount(2500.0).deadline(LocalDate.now().plusYears(1)).build();
    }

    // --- getAll ---

    @Test
    void getAll_returnsMappedList() {
        when(repo.findByUser(user)).thenReturn(List.of(buildGoal(1L, user)));

        List<SavingsGoalResponse> result = savingsGoalService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Vacation Fund");
    }

    @Test
    void getAll_whenEmpty_returnsEmptyList() {
        when(repo.findByUser(user)).thenReturn(List.of());

        assertThat(savingsGoalService.getAll()).isEmpty();
    }

    // --- create ---

    @Test
    void create_savesAndReturnsResponse() {
        SavingsGoal saved = buildGoal(10L, user);
        when(repo.save(any(SavingsGoal.class))).thenReturn(saved);

        SavingsGoalResponse resp = savingsGoalService.create(buildRequest());

        assertThat(resp).isNotNull();
        assertThat(resp.getTitle()).isEqualTo("Vacation Fund");
        assertThat(resp.getTargetAmount()).isEqualTo(10000.0);
        verify(repo).save(any(SavingsGoal.class));
    }

    // --- update ---

    @Test
    void update_ownGoal_updatesAndReturns() {
        SavingsGoal existing = buildGoal(1L, user);
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(existing)).thenReturn(existing);

        SavingsGoalRequest req = buildRequest();
        req.setTitle("Emergency Fund");
        req.setSavedAmount(5000.0);

        SavingsGoalResponse resp = savingsGoalService.update(1L, req);

        assertThat(resp.getTitle()).isEqualTo("Emergency Fund");
        assertThat(resp.getSavedAmount()).isEqualTo(5000.0);
        verify(repo).save(existing);
    }

    @Test
    void update_otherUsersGoal_throwsForbidden() {
        when(repo.findById(1L)).thenReturn(Optional.of(buildGoal(1L, otherUser)));

        assertThatThrownBy(() -> savingsGoalService.update(1L, buildRequest()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void update_notFound_throwsResourceNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> savingsGoalService.update(99L, buildRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- delete ---

    @Test
    void delete_ownGoal_deletesSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(buildGoal(1L, user)));

        savingsGoalService.delete(1L);

        verify(repo).deleteById(1L);
    }

    @Test
    void delete_otherUsersGoal_throwsForbidden() {
        when(repo.findById(1L)).thenReturn(Optional.of(buildGoal(1L, otherUser)));

        assertThatThrownBy(() -> savingsGoalService.delete(1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void delete_notFound_throwsResourceNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> savingsGoalService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

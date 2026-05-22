package com.budgettracker.budget_app.service;

import com.budgettracker.budget_app.exception.ForbiddenException;
import com.budgettracker.budget_app.exception.ResourceNotFoundException;
import com.budgettracker.budget_app.repository.EmiLoanRepository;
import com.budgettracker.budget_app.repository.UserRepository;
import com.budgettracker.budget_app.requestdto.EmiLoan;
import com.budgettracker.budget_app.requestdto.EmiLoanRequest;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.responsedto.EmiLoanResponse;
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
class EmiLoanServiceTest {

    @Mock private EmiLoanRepository repo;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private EmiLoanService emiLoanService;

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

    private EmiLoan buildLoan(Long id, UserRequest owner, int paidMonths) {
        return EmiLoan.builder()
                .id(id).user(owner).loanName("Home Loan")
                .principal(100000.0).interestRate(8.5).tenureMonths(12)
                .startDate(LocalDate.now().minusMonths(3)).paidMonths(paidMonths).build();
    }

    // --- getAll ---

    @Test
    void getAll_returnsMappedList() {
        when(repo.findByUser(user)).thenReturn(List.of(buildLoan(1L, user, 2)));

        List<EmiLoanResponse> result = emiLoanService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLoanName()).isEqualTo("Home Loan");
    }

    @Test
    void getAll_whenEmpty_returnsEmptyList() {
        when(repo.findByUser(user)).thenReturn(List.of());

        assertThat(emiLoanService.getAll()).isEmpty();
    }

    // --- create ---

    @Test
    void create_savesAndReturnsMappedResponse() {
        EmiLoanRequest req = EmiLoanRequest.builder()
                .loanName("Car Loan").principal(50000.0).interestRate(9.0)
                .tenureMonths(24).startDate(LocalDate.now()).build();
        EmiLoan saved = buildLoan(10L, user, 0);
        saved.setLoanName("Car Loan");
        saved.setPrincipal(50000.0);
        saved.setInterestRate(9.0);
        saved.setTenureMonths(24);
        when(repo.save(any(EmiLoan.class))).thenReturn(saved);

        EmiLoanResponse resp = emiLoanService.create(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getLoanName()).isEqualTo("Car Loan");
        verify(repo).save(any(EmiLoan.class));
    }

    // --- markPaid ---

    @Test
    void markPaid_incrementsPaidMonths() {
        EmiLoan loan = buildLoan(1L, user, 2);
        when(repo.findById(1L)).thenReturn(Optional.of(loan));
        when(repo.save(loan)).thenReturn(loan);

        EmiLoanResponse resp = emiLoanService.markPaid(1L);

        assertThat(resp.getPaidMonths()).isEqualTo(3);
    }

    @Test
    void markPaid_allAlreadyPaid_throwsIllegalState() {
        EmiLoan loan = buildLoan(1L, user, 12);
        when(repo.findById(1L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> emiLoanService.markPaid(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("All EMIs already paid");
    }

    @Test
    void markPaid_otherUsersLoan_throwsForbidden() {
        when(repo.findById(1L)).thenReturn(Optional.of(buildLoan(1L, otherUser, 0)));

        assertThatThrownBy(() -> emiLoanService.markPaid(1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void markPaid_notFound_throwsResourceNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emiLoanService.markPaid(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- undoPay ---

    @Test
    void undoPay_decrementsPaidMonths() {
        EmiLoan loan = buildLoan(1L, user, 3);
        when(repo.findById(1L)).thenReturn(Optional.of(loan));
        when(repo.save(loan)).thenReturn(loan);

        EmiLoanResponse resp = emiLoanService.undoPay(1L);

        assertThat(resp.getPaidMonths()).isEqualTo(2);
    }

    @Test
    void undoPay_noPaidMonths_throwsIllegalState() {
        EmiLoan loan = buildLoan(1L, user, 0);
        when(repo.findById(1L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> emiLoanService.undoPay(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No payments recorded to undo");
    }

    @Test
    void undoPay_otherUsersLoan_throwsForbidden() {
        when(repo.findById(1L)).thenReturn(Optional.of(buildLoan(1L, otherUser, 2)));

        assertThatThrownBy(() -> emiLoanService.undoPay(1L))
                .isInstanceOf(ForbiddenException.class);
    }

    // --- delete ---

    @Test
    void delete_ownLoan_deletesSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(buildLoan(1L, user, 0)));

        emiLoanService.delete(1L);

        verify(repo).deleteById(1L);
    }

    @Test
    void delete_otherUsersLoan_throwsForbidden() {
        when(repo.findById(1L)).thenReturn(Optional.of(buildLoan(1L, otherUser, 0)));

        assertThatThrownBy(() -> emiLoanService.delete(1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void delete_notFound_throwsResourceNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emiLoanService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

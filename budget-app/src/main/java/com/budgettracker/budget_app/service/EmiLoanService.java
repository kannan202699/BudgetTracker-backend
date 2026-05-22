package com.budgettracker.budget_app.service;

import com.budgettracker.budget_app.exception.ForbiddenException;
import com.budgettracker.budget_app.exception.ResourceNotFoundException;
import com.budgettracker.budget_app.repository.EmiLoanRepository;
import com.budgettracker.budget_app.repository.UserRepository;
import com.budgettracker.budget_app.requestdto.EmiLoan;
import com.budgettracker.budget_app.requestdto.EmiLoanRequest;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.responsedto.EmiLoanResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles EMI loan tracking: creation, payment recording, and balance calculation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmiLoanService {

    private final EmiLoanRepository repo;
    private final UserRepository userRepository;

    /**
     * Returns all EMI loans belonging to the current user.
     */
    public List<EmiLoanResponse> getAll() {
        log.debug("getAll - resolving current user");
        UserRequest user = currentUser();

        log.debug("getAll - fetching EMI loans for user: {}", user.getUsername());
        List<EmiLoanResponse> loans = repo.findByUser(user).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        log.info("getAll - returning {} EMI loan(s) for user: {}", loans.size(), user.getUsername());
        return loans;
    }

    /**
     * Creates a new EMI loan record for the current user.
     */
    @Transactional
    public EmiLoanResponse create(EmiLoanRequest req) {
        log.debug("create - resolving current user");
        UserRequest user = currentUser();

        log.debug("create - building EMI loan: loanName={}, principal={}, rate={}, tenure={}m, user={}",
                req.getLoanName(), req.getPrincipal(), req.getInterestRate(), req.getTenureMonths(), user.getUsername());

        EmiLoan loan = EmiLoan.builder()
                .user(user).loanName(req.getLoanName())
                .principal(req.getPrincipal()).interestRate(req.getInterestRate())
                .tenureMonths(req.getTenureMonths()).startDate(req.getStartDate())
                .paidMonths(0).build();

        EmiLoanResponse response = toResponse(repo.save(loan));
        log.info("create - EMI loan created: id={}, loanName={}, emi={}, user={}",
                response.getId(), response.getLoanName(), response.getEmiAmount(), user.getUsername());
        return response;
    }

    /**
     * Increments the paid-month counter for the given loan.
     */
    @Transactional
    public EmiLoanResponse markPaid(Long id) {
        log.debug("markPaid - fetching loan id={}", id);
        EmiLoan loan = findOwned(id);

        int paid = loan.getPaidMonths() == null ? 0 : loan.getPaidMonths();
        log.debug("markPaid - current paidMonths={}, tenure={} for loan id={}", paid, loan.getTenureMonths(), id);

        if (paid >= loan.getTenureMonths()) {
            log.warn("markPaid - all EMIs already paid for loan id={}, user={}",
                    id, loan.getUser().getUsername());
            throw new IllegalStateException("All EMIs already paid for this loan");
        }

        loan.setPaidMonths(paid + 1);
        EmiLoanResponse response = toResponse(repo.save(loan));
        log.info("markPaid - EMI payment recorded: loan id={}, paidMonths={}/{}, user={}",
                id, response.getPaidMonths(), response.getTenureMonths(), loan.getUser().getUsername());
        return response;
    }

    /**
     * Decrements the paid-month counter, undoing the last recorded payment.
     */
    @Transactional
    public EmiLoanResponse undoPay(Long id) {
        log.debug("undoPay - fetching loan id={}", id);
        EmiLoan loan = findOwned(id);

        int paid = loan.getPaidMonths() == null ? 0 : loan.getPaidMonths();
        log.debug("undoPay - current paidMonths={} for loan id={}", paid, id);

        if (paid <= 0) {
            log.warn("undoPay - no payments to undo for loan id={}, user={}", id, loan.getUser().getUsername());
            throw new IllegalStateException("No payments recorded to undo");
        }

        loan.setPaidMonths(paid - 1);
        EmiLoanResponse response = toResponse(repo.save(loan));
        log.info("undoPay - EMI payment undone: loan id={}, paidMonths={}/{}, user={}",
                id, response.getPaidMonths(), response.getTenureMonths(), loan.getUser().getUsername());
        return response;
    }

    /**
     * Deletes the specified EMI loan owned by the current user.
     */
    @Transactional
    public void delete(Long id) {
        log.debug("delete - fetching loan id={}", id);
        EmiLoan loan = findOwned(id);

        log.debug("delete - deleting loan id={}, loanName={}, user={}",
                id, loan.getLoanName(), loan.getUser().getUsername());
        repo.deleteById(loan.getId());
        log.info("delete - EMI loan deleted: id={}, loanName={}, user={}",
                id, loan.getLoanName(), loan.getUser().getUsername());
    }

    /**
     * Fetches the loan and verifies ownership; throws 404 or 403 accordingly.
     */
    private EmiLoan findOwned(Long id) {
        log.debug("findOwned - fetching loan id={}", id);
        EmiLoan loan = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("findOwned - loan not found: id={}", id);
                    return new ResourceNotFoundException("Loan not found");
                });

        UserRequest user = currentUser();
        if (!loan.getUser().getId().equals(user.getId())) {
            log.warn("findOwned - access denied: user={} attempted to access loan id={} owned by userId={}",
                    user.getUsername(), id, loan.getUser().getId());
            throw new ForbiddenException("Access denied");
        }

        log.debug("findOwned - ownership verified: loan id={}, user={}", id, user.getUsername());
        return loan;
    }

    /**
     * Computes EMI, totals, remaining balance, and next due date from the loan entity.
     */
    private EmiLoanResponse toResponse(EmiLoan loan) {
        double P = loan.getPrincipal();
        double annualRate = loan.getInterestRate();
        int n = loan.getTenureMonths();
        double r = (annualRate / 100.0) / 12.0;

        double emi;
        if (r == 0) {
            emi = P / n;
        } else {
            double factor = Math.pow(1 + r, n);
            emi = P * r * factor / (factor - 1);
        }
        emi = Math.round(emi * 100.0) / 100.0;

        double totalAmount = Math.round(emi * n * 100.0) / 100.0;
        double totalInterest = Math.round((totalAmount - P) * 100.0) / 100.0;

        LocalDate now = LocalDate.now();
        long calendarElapsed = Math.max(0, Math.min(ChronoUnit.MONTHS.between(loan.getStartDate(), now), n));

        int paid = loan.getPaidMonths() == null ? 0 : loan.getPaidMonths();
        long remaining = n - paid;

        double remainingBalance;
        if (r == 0) {
            remainingBalance = P - (emi * paid);
        } else {
            double factor = Math.pow(1 + r, paid);
            remainingBalance = P * factor - emi * (factor - 1) / r;
        }
        remainingBalance = Math.max(0, Math.round(remainingBalance * 100.0) / 100.0);

        LocalDate nextDue = paid < n ? loan.getStartDate().plusMonths(paid + 1) : null;

        log.debug("toResponse - loan id={}: emi={}, paid={}/{}, remainingBalance={}, nextDue={}",
                loan.getId(), emi, paid, n, remainingBalance, nextDue);

        return EmiLoanResponse.builder()
                .id(loan.getId()).loanName(loan.getLoanName())
                .principal(P).interestRate(annualRate).tenureMonths(n).startDate(loan.getStartDate())
                .emiAmount(emi).totalAmount(totalAmount).totalInterest(totalInterest)
                .paidMonths(paid)
                .monthsElapsed(calendarElapsed)
                .monthsRemaining(remaining)
                .remainingBalance(remainingBalance).nextDueDate(nextDue).build();
    }

    /**
     * Resolves the authenticated user from the security context; throws if not found.
     */
    private UserRequest currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("currentUser - resolving user from security context: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("currentUser - authenticated user not found in DB: {}", username);
                    return new UsernameNotFoundException("User not found");
                });
    }
}

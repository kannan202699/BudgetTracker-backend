package com.budgettracker.budget_app;

import com.budgettracker.budget_app.repository.UserRepository;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.util.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BudgetAppApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    void contextLoads() {
        // Verifies the full Spring context starts successfully with H2
    }

    @Test
    void dataInitializer_createsAdminOnStartup() {
        // DataInitializer runs on startup — verify the admin user was created
        Optional<UserRequest> admin = userRepository.findByUsername("admin");
        assertThat(admin).isPresent();
        assertThat(admin.get().getRole()).isEqualTo(Role.ADMIN);
    }
}

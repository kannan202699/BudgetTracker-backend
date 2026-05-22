package com.budgettracker.budget_app.config;

import com.budgettracker.budget_app.repository.UserRepository;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.util.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the database with the admin account on application startup, creating or updating as needed.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates the admin account if absent, or updates the password when it has changed.
     */
    @Override
    public void run(String... args) {

        userRepository.findByUsername(adminUsername).ifPresentOrElse(
                existing -> {
                    if (!passwordEncoder.matches(adminPassword, existing.getPassword())) {
                        existing.setPassword(passwordEncoder.encode(adminPassword));
                        userRepository.save(existing);
                        System.out.println("✅ Admin password updated");
                    } else {
                        System.out.println("ℹ️ Admin already exists");
                    }
                },
                () -> {
                    UserRequest admin = new UserRequest();
                    admin.setUsername(adminUsername);
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    admin.setRole(Role.ADMIN);
                    userRepository.save(admin);
                    System.out.println("✅ Admin user created");
                }
        );
    }

}

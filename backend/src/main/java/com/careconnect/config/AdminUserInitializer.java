package com.careconnect.config;

import com.careconnect.entity.Role;
import com.careconnect.entity.User;
import com.careconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Startup initializer to seed an initial ADMIN user if one does not exist.
 * Reads credentials securely from environment variables (ADMIN_INITIAL_EMAIL and ADMIN_INITIAL_PASSWORD)
 * and hashes the password with BCrypt before storage.
 * Does NOT contain any hardcoded real passwords in source control.
 */
@Component
public class AdminUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.admin-email:}")
    private String adminEmail;

    @Value("${app.init.admin-password:}")
    private String adminPassword;

    public AdminUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        long adminCount = userRepository.countByRole(Role.ADMIN);
        if (adminCount == 0) {
            if (adminEmail != null && !adminEmail.isBlank() && adminPassword != null && !adminPassword.isBlank()) {
                log.info("No ADMIN found. Bootstrapping initial ADMIN user from environment variables: {}", adminEmail);
                User admin = new User(
                        adminEmail.trim().toLowerCase(),
                        passwordEncoder.encode(adminPassword),
                        Role.ADMIN,
                        true
                );
                userRepository.save(admin);
                log.info("Initial ADMIN user successfully created with hashed BCrypt credentials.");
            } else {
                log.info("No ADMIN account exists. To seed an initial admin account automatically, " +
                        "provide ADMIN_INITIAL_EMAIL and ADMIN_INITIAL_PASSWORD environment variables, " +
                        "or execute the documented script in 'src/main/resources/schema-seed-admin.sql'.");
            }
        } else {
            log.debug("ADMIN account(s) already exist ({}); skipping initial admin seed.", adminCount);
        }
    }
}

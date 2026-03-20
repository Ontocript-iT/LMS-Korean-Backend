package com.lms.ontocriptIT.backend.config;

import com.lms.ontocriptIT.backend.entity.AccountStatus;
import com.lms.ontocriptIT.backend.entity.Role;
import com.lms.ontocriptIT.backend.entity.User;
import com.lms.ontocriptIT.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create default admin if not exists
        if (!userRepository.existsByEmail("admin@lms.com")) {
            User admin = User.builder()
                    .studentId("ADMIN001")
                    .firstName("Admin")
                    .lastName("User")
                    .district("Colombo")
                    .idNumber("000000000V")
                    .email("admin@lms.com")
                    .phoneNumber1("0771234567")
                    .phoneNumber2("0771234568")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .status(AccountStatus.ACTIVE)
                    .isTemporaryPassword(false)
                    .build();

            userRepository.save(admin);
            System.out.println("Default admin user created!");
        }
    }
}

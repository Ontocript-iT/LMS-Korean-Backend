package com.lms.ontocriptIT.backend.config;

import com.lms.ontocriptIT.backend.entity.AccountStatus;
import com.lms.ontocriptIT.backend.entity.Role;
import com.lms.ontocriptIT.backend.entity.User;
import com.lms.ontocriptIT.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create default admin if not exists
        if (!userRepository.existsByEmail("admin1@lms.com")) {
            User admin = User.builder()
                    .studentId("ADMIN002")
                    .firstName("Admin")
                    .lastName("User")
                    .district("Colombo")
                    .idNumber("000000001V")
                    .email("admin1@lms.com")
                    .phoneNumber1("0771234467")
                    .phoneNumber2("0771234368")
                    .password(passwordEncoder.encode("admin123kEps"))
                    .role(Role.ADMIN)
                    .status(AccountStatus.ACTIVE)
                    .isTemporaryPassword(false)
                    .build();

            userRepository.save(admin);
            System.out.println("Default admin user created!");
        }

        Optional<User> user=userRepository.findByEmail("admin@lms.com");
        userRepository.delete(user.get());
    }
}

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
        if (!userRepository.existsByEmail("admin2@lms.com")) {
            User admin = User.builder()
                    .studentId("ADMIN003")
                    .firstName("Admin")
                    .lastName("User")
                    .district("Colombo")
                    .idNumber("000000300V")
                    .email("admin2@lms.com")
                    .phoneNumber1("0771134567")
                    .phoneNumber2("0771134568")
                    .password(passwordEncoder.encode("addffsfsdfsdfs@D"))
                    .role(Role.ADMIN)
                    .status(AccountStatus.ACTIVE)
                    .isTemporaryPassword(false)
                    .build();

            userRepository.save(admin);
            System.out.println("Default admin user created!");
        }
    }
}

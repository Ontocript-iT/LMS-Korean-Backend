package com.lms.ontocriptIT.backend.security;

import com.lms.ontocriptIT.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Try to find user by studentId, email, or phone number
        return userRepository.findByStudentId(username)
                .or(() -> userRepository.findByEmail(username))
                .or(() -> userRepository.findByPhoneNumber1(username))
                .or(() -> userRepository.findByPhoneNumber2(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}

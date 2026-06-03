package com.mtnrs.revenuesync.service;

import com.mtnrs.revenuesync.domain.User;
import com.mtnrs.revenuesync.domain.enums.UserRole;
import com.mtnrs.revenuesync.infra.exception.BusinessException;
import com.mtnrs.revenuesync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtService            jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService          emailService;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public void registerUser(String name, String email, String password) {
        if (userRepository.existsByEmail(email))
            throw new BusinessException("Email already in use");

        String token = String.format("%06d", RANDOM.nextInt(1_000_000));

        User user = User.of(name, email, passwordEncoder.encode(password), UserRole.USER);
        user.setVerificationToken(token);
        userRepository.save(user);

        emailService.sendVerificationEmail(email, name, token);
    }

    @Transactional
    public String verifyEmail(String email, String token) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!user.verifyEmail(token))
            throw new BusinessException("Invalid or expired verification token");

        userRepository.save(user);
        return jwtService.generateToken(user);
    }

    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!user.isEmailVerified())
            throw new BusinessException("Please verify your email before logging in");

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));

        return jwtService.generateToken(user);
    }
}

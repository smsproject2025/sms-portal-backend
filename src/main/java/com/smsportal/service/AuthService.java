package com.smsportal.service;

import com.smsportal.dto.*;
import com.smsportal.model.User;
import com.smsportal.model.Wallet;
import com.smsportal.repository.UserRepository;
import com.smsportal.repository.WalletRepository;
import com.smsportal.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final EmailService emailService;

    // ── Register ─────────────────────────────────────────────────────

    @Transactional
    public ApiResponse<AuthResponseDTO> register(RegisterRequestDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            return ApiResponse.error("Email already registered");
        }
        if (userRepository.existsByMobile(dto.getMobile())) {
            return ApiResponse.error("Mobile number already registered");
        }

        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .mobile(dto.getMobile())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(User.Role.USER)
                .verified(true)
                .apiKey(UUID.randomUUID().toString().replace("-", ""))
                .build();

        user = userRepository.save(user);

        Wallet wallet = Wallet.builder().user(user).build();
        walletRepository.save(wallet);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token        = jwtTokenProvider.generateToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        log.info("New user registered: {}", user.getEmail());

        return ApiResponse.success(
                AuthResponseDTO.builder()
                        .token(token).refreshToken(refreshToken)
                        .userId(user.getId()).name(user.getName())
                        .email(user.getEmail()).role(user.getRole().name())
                        .walletBalance(0.0).build(),
                "Registration successful");
    }

    // ── Login ─────────────────────────────────────────────────────────

    public ApiResponse<AuthResponseDTO> login(AuthRequestDTO dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword()));

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isActive()) {
            return ApiResponse.error("Account is deactivated. Contact support.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token        = jwtTokenProvider.generateToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        Wallet wallet  = walletRepository.findByUser(user).orElse(null);
        double balance = wallet != null ? wallet.getBalance() : 0.0;

        return ApiResponse.success(
                AuthResponseDTO.builder()
                        .token(token).refreshToken(refreshToken)
                        .userId(user.getId()).name(user.getName())
                        .email(user.getEmail()).role(user.getRole().name())
                        .walletBalance(balance).build(),
                "Login successful");
    }

    // ── Refresh Token ─────────────────────────────────────────────────

    public ApiResponse<AuthResponseDTO> refreshToken(String refreshToken) {
        String email      = jwtTokenProvider.extractUsername(refreshToken);
        UserDetails ud    = userDetailsService.loadUserByUsername(email);

        if (!jwtTokenProvider.isTokenValid(refreshToken, ud)) {
            return ApiResponse.error("Invalid or expired refresh token");
        }

        String newToken = jwtTokenProvider.generateToken(ud);
        User user       = userRepository.findByEmail(email).orElseThrow();

        return ApiResponse.success(
                AuthResponseDTO.builder()
                        .token(newToken).refreshToken(refreshToken)
                        .userId(user.getId()).name(user.getName())
                        .email(user.getEmail()).role(user.getRole().name()).build(),
                "Token refreshed");
    }

    // ── Forgot Password ───────────────────────────────────────────────

    @Transactional
    public ApiResponse<String> forgotPassword(String email) {
        // Always return the same message so we don't leak whether the email exists
        String genericMsg = "If this email is registered, a reset link has been sent.";

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.info("Password reset requested for unknown email: {}", email);
            return ApiResponse.success(genericMsg, "OK");
        }

        // Invalidate any existing token and create a fresh one
        String resetToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        // Send reset email asynchronously
        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetToken);
        System.out.println("email sent");

        log.info("Password reset email queued for: {}", email);
        return ApiResponse.success(genericMsg, "OK");
    }

    // ── Reset Password ────────────────────────────────────────────────

    @Transactional
    public ApiResponse<String> resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            return ApiResponse.error("Reset token is required");
        }
        if (newPassword == null || newPassword.length() < 8) {
            return ApiResponse.error("Password must be at least 8 characters");
        }

        User user = userRepository.findByResetPasswordToken(token).orElse(null);

        if (user == null) {
            return ApiResponse.error("Invalid or already used reset link");
        }
        if (user.getResetPasswordTokenExpiry() == null ||
            user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            return ApiResponse.error("Reset link has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);

        // Notify user of the password change
        emailService.sendPasswordChangedEmail(user.getEmail(), user.getName());

        log.info("Password reset successfully for: {}", user.getEmail());
        return ApiResponse.success("Password reset successfully. You can now log in.", "OK");
    }
}

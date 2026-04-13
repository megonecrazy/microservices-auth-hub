package com.authservice.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.authservice.dto.AuthRequest;
import com.authservice.dto.AuthResponse;
import com.authservice.dto.OtpVerifyRequest;
import com.authservice.dto.RegisterRequest;
import com.authservice.event.KafkaEventPublisher;
import com.authservice.event.UserRegisteredEvent;
import com.authservice.exception.AccountNotVerifiedException;
import com.authservice.exception.InvalidCredentialsException;
import com.authservice.exception.InvalidOtpException;
import com.authservice.exception.OtpExpiredException;
import com.authservice.exception.UserAlreadyExistsException;
import com.authservice.exception.UserNotFoundException;
import com.authservice.model.OtpToken;
import com.authservice.model.User;
import com.authservice.repo.OtpRepository;
import com.authservice.repo.UserRepository;
import com.authservice.util.JwtProvider;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;

    private final UserRepository userRepo;
    private final OtpRepository otpRepo;
    private final PasswordEncoder encoder;
    private final JwtProvider jwtProvider;
    private final KafkaEventPublisher eventPublisher;

    public AuthService(UserRepository userRepo,
                       OtpRepository otpRepo,
                       PasswordEncoder encoder,
                       JwtProvider jwtProvider,
                       KafkaEventPublisher eventPublisher) {
        this.userRepo = userRepo;
        this.otpRepo = otpRepo;
        this.encoder = encoder;
        this.jwtProvider = jwtProvider;
        this.eventPublisher = eventPublisher;
    }

    // ── Register ──
    @Transactional
    public void register(RegisterRequest request) {
        // Check for duplicate username
        if (userRepo.findByUsername(request.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException(
                    "Username '" + request.getUsername() + "' is already taken");
        }

        // Check for duplicate email
        if (userRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException(
                    "Email '" + request.getEmail() + "' is already registered");
        }

        // Build and save user (disabled until OTP verified)
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(encoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : "ROLE_USER")
                .emailVerified(false)
                .enabled(false)
                .build();

        userRepo.save(user);
        log.info("User registered: {} ({})", user.getUsername(), user.getEmail());

        // Generate OTP and publish event
        String otpCode = generateOtp();
        saveOtpToken(user.getEmail(), otpCode, OtpToken.Purpose.REGISTRATION);
        publishRegistrationEvent(user, otpCode);
    }

    // ── Verify OTP ──
    @Transactional
    public void verifyOtp(OtpVerifyRequest request) {
        OtpToken otp = otpRepo
                .findTopByEmailAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
                        request.getEmail(), OtpToken.Purpose.REGISTRATION)
                .orElseThrow(() -> new InvalidOtpException(
                        "No pending OTP found for this email"));

        if (otp.isExpired()) {
            throw new OtpExpiredException(
                    "OTP has expired. Please request a new one.");
        }

        if (!otp.getOtpCode().equals(request.getOtpCode())) {
            throw new InvalidOtpException("Invalid OTP code");
        }

        // Mark OTP as verified
        otp.setVerified(true);
        otpRepo.save(otp);

        // Enable the user account
        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setEmailVerified(true);
        user.setEnabled(true);
        userRepo.save(user);

        log.info("Email verified for user: {}", user.getUsername());
    }

    // ── Resend OTP ──
    @Transactional
    public void resendOtp(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(
                        "No account found with email: " + email));

        if (user.isEmailVerified()) {
            throw new InvalidOtpException("Email is already verified");
        }

        String otpCode = generateOtp();
        saveOtpToken(email, otpCode, OtpToken.Purpose.REGISTRATION);
        publishRegistrationEvent(user, otpCode);

        log.info("OTP resent to: {}", email);
    }

    // ── Login ──
    public AuthResponse login(AuthRequest request) {
        User user = userRepo.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!encoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        if (!user.isEmailVerified()) {
            throw new AccountNotVerifiedException(
                    "Please verify your email before logging in. Check your inbox for the OTP.");
        }

        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        log.info("User logged in: {}", user.getUsername());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // ── Helpers ──

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // 6-digit: 100000–999999
        return String.valueOf(code);
    }

    private void saveOtpToken(String email, String otpCode, OtpToken.Purpose purpose) {
        OtpToken token = OtpToken.builder()
                .email(email)
                .otpCode(otpCode)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .verified(false)
                .createdAt(LocalDateTime.now())
                .build();
        otpRepo.save(token);
    }

    private void publishRegistrationEvent(User user, String otpCode) {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .otpCode(otpCode)
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishUserRegistered(event);
    }
}
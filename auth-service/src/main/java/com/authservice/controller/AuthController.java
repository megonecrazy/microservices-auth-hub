package com.authservice.controller;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.authservice.dto.AuthRequest;
import com.authservice.dto.AuthResponse;
import com.authservice.dto.OtpVerifyRequest;
import com.authservice.dto.RegisterRequest;
import com.authservice.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        service.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message",
                        "Registration successful. Please check your email for the OTP verification code."));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        service.verifyOtp(request);
        return ResponseEntity.ok(
                Map.of("message", "Email verified successfully. You can now log in."));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(@RequestParam String email) {
        service.resendOtp(email);
        return ResponseEntity.ok(
                Map.of("message", "A new OTP has been sent to your email."));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse authResponse = service.login(request);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + authResponse.getAccessToken());
        headers.set("X-Refresh-Token", authResponse.getRefreshToken());
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Authorization, X-Refresh-Token");

        return ResponseEntity.ok()
                .headers(headers)
                .body(Map.of("message", "Logged in successfully"));
    }
}
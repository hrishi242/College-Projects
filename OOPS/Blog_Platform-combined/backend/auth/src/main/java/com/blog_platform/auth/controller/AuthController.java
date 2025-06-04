package com.blog_platform.auth.controller;



import com.blog_platform.auth.dto.AuthResponse;
import com.blog_platform.auth.dto.LoginRequest;
import com.blog_platform.auth.dto.OtpVerificationRequest;
import com.blog_platform.auth.dto.SignupRequest;
import com.blog_platform.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest signupRequest) {
        return authService.initiateSignup(signupRequest);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpVerificationRequest otpRequest) {
        return authService.verifyOtpAndCompleteSignup(otpRequest);
    }

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.authenticate(loginRequest);
    }
}
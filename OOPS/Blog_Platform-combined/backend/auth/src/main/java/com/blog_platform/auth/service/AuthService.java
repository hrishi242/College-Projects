package com.blog_platform.auth.service;


import com.blog_platform.auth.dto.*;
import com.blog_platform.auth.model.CustomUserDetails;
import com.blog_platform.auth.model.User;
import com.blog_platform.auth.repository.UserRepository;
import com.blog_platform.auth.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final OtpService otpService;
    private final RedisService redisService;
    private final AuthenticationManager authenticationManager;

    public ResponseEntity<?> initiateSignup(SignupRequest signupRequest) {
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            return ResponseEntity.badRequest().body(
                    new ErrorResponse(
                            LocalDateTime.now(),
                            HttpStatus.BAD_REQUEST.value(),
                            "Validation Error",
                            "Username is already taken",
                            "/api/auth/signup"
                    )
            );
        }
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return ResponseEntity.badRequest().body(
                    new ErrorResponse(
                            LocalDateTime.now(),
                            HttpStatus.BAD_REQUEST.value(),
                            "Validation Error",
                            "Email is already in use",
                            "/api/auth/signup"
                    )
            );
        }

        redisService.storeTempUser(signupRequest.getUsername(), signupRequest);
        otpService.sendOtp(signupRequest.getUsername(), signupRequest.getEmail());
        return ResponseEntity.ok("OTP sent to your email");
    }

    public ResponseEntity<?> verifyOtpAndCompleteSignup(OtpVerificationRequest otpRequest) {
        if (!otpService.verifyOtp(otpRequest.getUsername(), otpRequest.getOtp())) {
            return ResponseEntity.badRequest().body(
                    new ErrorResponse(
                            LocalDateTime.now(),
                            HttpStatus.BAD_REQUEST.value(),
                            "Validation Error",
                            "Invalid or expired OTP",
                            "/api/auth/verify-otp"
                    )
            );
        }

        SignupRequest signupRequest = redisService.getTempUser(otpRequest.getUsername());
        if (signupRequest == null) {
            return ResponseEntity.badRequest().body(
                    new ErrorResponse(
                            LocalDateTime.now(),
                            HttpStatus.BAD_REQUEST.value(),
                            "Validation Error",
                            "Signup session expired",
                            "/api/auth/verify-otp"
                    )
            );
        }

        User user = new User();
        user.setUsername(signupRequest.getUsername());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setTopics(signupRequest.getTopics());

        userRepository.save(user);
        redisService.deleteTempUser(otpRequest.getUsername());

        return ResponseEntity.ok(new AuthResponse(
                jwtProvider.generateToken(new CustomUserDetails(user)),
                user.getUsername(),
                user.getTopics()
        ));
    }

    public ResponseEntity<?> authenticate(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
            return ResponseEntity.ok(new AuthResponse(
                    jwtProvider.generateToken(new CustomUserDetails(user)),
                    user.getUsername(),
                    user.getTopics()
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest().body(
                    new ErrorResponse(
                            LocalDateTime.now(),
                            HttpStatus.BAD_REQUEST.value(),
                            "Validation Error",
                            "Invalid username or password",
                            "/api/auth/signin"
                    )
            );
        }
    }
}

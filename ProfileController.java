package com.tarundb.authify.controller;

import com.tarundb.authify.io.OtpRequest;
import com.tarundb.authify.io.ProfileRequest;
import com.tarundb.authify.io.ProfileResponse;
import com.tarundb.authify.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Step 1 of registration: create account + auto-send OTP to email.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse register(@Valid @RequestBody ProfileRequest request) {
        return profileService.createProfile(request);
    }

    /**
     * Step 2 of registration: verify the OTP to confirm registration.
     */
    @PostMapping("/verify-registration-otp")
    public ResponseEntity<?> verifyRegistrationOtp(@RequestBody OtpRequest request) {
        ProfileResponse response = profileService.verifyRegistrationOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Registration successful! You can now log in.",
                "user", response
        ));
    }

    /**
     * Resend registration OTP (in case user didn't receive it).
     */
    @PostMapping("/resend-registration-otp")
    public ResponseEntity<?> resendRegistrationOtp(@RequestBody Map<String, String> body) {
        profileService.sendRegistrationOtp(body.get("email"));
        return ResponseEntity.ok(Map.of("message", "OTP resent successfully"));
    }

    /**
     * Authenticated: send OTP to verify account email.
     */
    @PostMapping("/send-verify-otp")
    public ResponseEntity<?> sendVerifyOtp() {
        String email = getAuthenticatedEmail();
        profileService.sendVerifyEmail(email);
        return ResponseEntity.ok(Map.of("message", "OTP sent to your email"));
    }

    /**
     * Authenticated: verify the account email with OTP.
     */
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody OtpRequest request) {
        String email = getAuthenticatedEmail();
        profileService.verifyEmail(email, request.getOtp());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Email verified successfully!"
        ));
    }

    /**
     * Authenticated: get current user's profile.
     */
    @GetMapping("/profile")
    public ProfileResponse getProfile() {
        String email = getAuthenticatedEmail();
        return profileService.getProfile(email);
    }

    // ── Password Reset (public – no JWT required) ─────────────────────────────

    /**
     * Step 1: Send a 6-digit OTP to the given email for password reset.
     */
    @PostMapping("/send-reset-otp")
    public ResponseEntity<?> sendResetOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }
        profileService.sendResetOtp(email.trim());
        return ResponseEntity.ok(Map.of("message", "OTP sent to " + email));
    }

    /**
     * Step 2: Verify the OTP (without changing the password yet).
     */
    @PostMapping("/verify-reset-otp")
    public ResponseEntity<?> verifyResetOtp(@RequestBody Map<String, String> body) {
        profileService.verifyResetOtp(body.get("email"), body.get("otp"));
        return ResponseEntity.ok(Map.of("success", true, "message", "OTP verified"));
    }

    /**
     * Step 3: Set the new password (re-validates OTP for safety).
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        profileService.resetPassword(body.get("email"), body.get("otp"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("success", true, "message", "Password reset successfully"));
    }

    private String getAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}


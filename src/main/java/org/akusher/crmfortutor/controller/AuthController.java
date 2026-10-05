package org.akusher.crmfortutor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.dto.request.LoginRequest;
import org.akusher.crmfortutor.dto.request.RefreshTokenRequest;
import org.akusher.crmfortutor.dto.request.RegisterRequest;
import org.akusher.crmfortutor.dto.request.StudentRegisterRequest;
import org.akusher.crmfortutor.dto.response.AuthResponse;
import org.akusher.crmfortutor.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.akusher.crmfortutor.dto.request.ChangePasswordRequest;
import org.akusher.crmfortutor.service.ProfileService;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ProfileService profileService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/register-student")
    public ResponseEntity<AuthResponse> registerStudent(@Valid @RequestBody StudentRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerStudent(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(request);
        return ResponseEntity.ok(Map.of("message", "Пароль успешно изменен"));
    }
}

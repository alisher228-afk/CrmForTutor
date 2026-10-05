package org.akusher.crmfortutor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.dto.request.ChangePasswordRequest;
import org.akusher.crmfortutor.dto.request.UserProfileUpdateRequest;
import org.akusher.crmfortutor.dto.response.UserProfileResponse;
import org.akusher.crmfortutor.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile() {
        return ResponseEntity.ok(profileService.getProfile());
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(request);
        return ResponseEntity.ok(Map.of("message", "Пароль успешно изменен"));
    }
}

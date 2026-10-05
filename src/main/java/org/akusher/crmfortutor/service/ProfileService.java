package org.akusher.crmfortutor.service;

import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.dto.request.ChangePasswordRequest;
import org.akusher.crmfortutor.dto.request.UserProfileUpdateRequest;
import org.akusher.crmfortutor.dto.response.UserProfileResponse;
import org.akusher.crmfortutor.entity.Role;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.entity.User;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.repository.StudentProfileRepository;
import org.akusher.crmfortutor.repository.UserRepository;
import org.akusher.crmfortutor.security.CurrentUserProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile() {
        Long userId = currentUserProvider.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String phone = user.getPhone();

        if (user.getRole() == Role.ROLE_STUDENT) {
            Optional<StudentProfile> studentProfileOpt = studentProfileRepository.findByUserId(userId);
            if (studentProfileOpt.isPresent()) {
                StudentProfile studentProfile = studentProfileOpt.get();
                if (firstName == null || firstName.isBlank()) {
                    firstName = studentProfile.getFirstName();
                }
                if (lastName == null || lastName.isBlank()) {
                    lastName = studentProfile.getLastName();
                }
                if (phone == null || phone.isBlank()) {
                    phone = studentProfile.getPhone();
                }
            }
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .firstName(firstName)
                .lastName(lastName)
                .phone(phone)
                .specialization(user.getSpecialization())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public UserProfileResponse updateProfile(UserProfileUpdateRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getSpecialization() != null) {
            user.setSpecialization(request.getSpecialization().trim());
        }

        if (user.getRole() == Role.ROLE_STUDENT) {
            studentProfileRepository.findByUserId(userId).ifPresent(studentProfile -> {
                if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
                    studentProfile.setFirstName(request.getFirstName().trim());
                }
                if (request.getLastName() != null) {
                    studentProfile.setLastName(request.getLastName().trim());
                }
                if (request.getPhone() != null) {
                    studentProfile.setPhone(request.getPhone().trim());
                }
                studentProfileRepository.save(studentProfile);
            });
        }

        User updatedUser = userRepository.save(user);

        return UserProfileResponse.builder()
                .id(updatedUser.getId())
                .email(updatedUser.getEmail())
                .role(updatedUser.getRole())
                .firstName(updatedUser.getFirstName())
                .lastName(updatedUser.getLastName())
                .phone(updatedUser.getPhone())
                .specialization(updatedUser.getSpecialization())
                .createdAt(updatedUser.getCreatedAt())
                .build();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Текущий пароль указан неверно");
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new BadRequestException("Новый пароль должен отличаться от текущего");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}

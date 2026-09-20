package org.akusher.crmfortutor.security;

import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.repository.StudentProfileRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentStudentProvider {

    private final CurrentUserProvider currentUserProvider;
    private final StudentProfileRepository studentProfileRepository;

    /**
     * Resolves the StudentProfile of the currently authenticated student user.
     *
     * @return StudentProfile of current user
     * @throws AccessDeniedException if current user is not linked to any StudentProfile (results in 403)
     */
    public StudentProfile getCurrentStudentProfile() {
        Long userId = currentUserProvider.getCurrentUserId();
        return studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("No student profile found for current user"));
    }

    /**
     * Retrieves the ID of the currently authenticated student's profile.
     *
     * @return current student profile's ID
     */
    public Long getCurrentStudentProfileId() {
        return getCurrentStudentProfile().getId();
    }
}

package org.akusher.crmfortutor.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    /**
     * Retrieves the ID of the currently authenticated user (tutor or student).
     *
     * @return current user's ID
     * @throws AccessDeniedException if user is not authenticated
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }

        throw new AccessDeniedException("Unable to determine current user ID from security context");
    }

    /**
     * Alias for {@link #getCurrentUserId()} representing the current tutor's ID.
     *
     * @return current tutor's ID
     */
    public Long getCurrentTutorId() {
        return getCurrentUserId();
    }

    /**
     * Retrieves the currently authenticated {@link UserPrincipal}.
     *
     * @return current UserPrincipal
     * @throws AccessDeniedException if user is not authenticated
     */
    public UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }
        throw new AccessDeniedException("User is not authenticated");
    }

    /**
     * Retrieves the email of the currently authenticated user.
     *
     * @return email of current user
     */
    public String getCurrentUserEmail() {
        return getCurrentUser().getEmail();
    }
}

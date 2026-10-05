package org.akusher.crmfortutor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.akusher.crmfortutor.entity.Role;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private Long id;
    private String email;
    private Role role;
    private String firstName;
    private String lastName;
    private String phone;
    private String specialization;
    private Instant createdAt;
}

package org.akusher.crmfortutor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.akusher.crmfortutor.entity.StudentStatus;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSelfResponse {
    private Long id;
    private Long userId;
    private Long tutorId;
    private String firstName;
    private String lastName;
    private String phone;
    private String telegram;
    private String currentLevel;
    private BigDecimal hourlyRate;
    private Integer lessonBalance;
    private StudentStatus status;
}

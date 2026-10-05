package org.akusher.crmfortutor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.akusher.crmfortutor.entity.TestType;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestResponse {

    private Long id;
    private Long tutorId;
    private String title;
    private String description;
    private String topic;
    private TestType type;
    private String externalUrl;
    private String questionsJson;
    private Integer timeLimitMinutes;
    private Instant deadline;
    private org.akusher.crmfortutor.entity.TestTargetType targetType;
    private String groupName;
    private Long studentId;
    private String studentName;
    private Long submissionsCount;
    private Double averageScore;
    private TestSubmissionResponse mySubmission;
    private Instant createdAt;
    private Instant updatedAt;
}

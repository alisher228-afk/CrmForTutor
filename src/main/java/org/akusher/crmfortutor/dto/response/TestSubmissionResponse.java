package org.akusher.crmfortutor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestSubmissionResponse {

    private Long id;
    private Long testId;
    private String testTitle;
    private Long studentId;
    private String studentName;
    private Integer score;
    private Integer totalQuestions;
    private Integer percentage;
    private Integer timeSpentSeconds;
    private String answersJson;
    private Instant submittedAt;
}

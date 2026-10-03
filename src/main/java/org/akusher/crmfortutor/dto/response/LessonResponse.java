package org.akusher.crmfortutor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.akusher.crmfortutor.entity.LessonStatus;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonResponse {
    private Long id;
    private Long tutorId;
    private Long studentId;
    private String studentFirstName;
    private String studentLastName;
    private String studentName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LessonStatus status;
    private String topic;
    private String meetingUrl;
    private String groupName;
    private String cancellationReason;
    private Instant reminderSentAt;

    public String getStudentName() {
        if (studentName != null && !studentName.isBlank()) {
            return studentName;
        }
        String first = studentFirstName != null ? studentFirstName.trim() : "";
        String last = studentLastName != null ? studentLastName.trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? null : full;
    }
}

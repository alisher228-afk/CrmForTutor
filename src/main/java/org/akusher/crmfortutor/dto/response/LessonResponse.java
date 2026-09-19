package org.akusher.crmfortutor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.akusher.crmfortutor.entity.LessonStatus;

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
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LessonStatus status;
    private String topic;
    private String meetingUrl;
}

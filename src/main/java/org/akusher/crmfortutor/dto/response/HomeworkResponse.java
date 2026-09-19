package org.akusher.crmfortutor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.akusher.crmfortutor.entity.HomeworkStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeworkResponse {
    private Long id;
    private Long lessonId;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private HomeworkStatus status;
    private String studentNotes;
}

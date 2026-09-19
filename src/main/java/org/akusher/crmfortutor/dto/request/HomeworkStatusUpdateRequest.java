package org.akusher.crmfortutor.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.akusher.crmfortutor.entity.HomeworkStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeworkStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private HomeworkStatus status;

    private String studentNotes;
}

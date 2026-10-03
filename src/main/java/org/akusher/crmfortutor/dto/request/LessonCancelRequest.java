package org.akusher.crmfortutor.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonCancelRequest {

    @Size(max = 500, message = "Причина отмены не должна превышать 500 символов")
    private String reason;
}

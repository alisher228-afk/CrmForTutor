package org.akusher.crmfortutor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.akusher.crmfortutor.entity.TestType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String topic;

    @NotNull(message = "Type is required")
    private TestType type;

    private String externalUrl;

    private String questionsJson;

    private Integer timeLimitMinutes;
}

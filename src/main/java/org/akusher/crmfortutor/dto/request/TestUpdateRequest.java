package org.akusher.crmfortutor.dto.request;

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
public class TestUpdateRequest {

    private String title;

    private String description;

    private String topic;

    private TestType type;

    private String externalUrl;

    private String questionsJson;

    private Integer timeLimitMinutes;
}

package org.akusher.crmfortutor.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class TelegramLinkCodeResponse {

    private Long studentId;
    private String linkCode;
    private Instant expiresAt;
    private String botUsername;

    public String getCode() {
        return linkCode;
    }

    @JsonProperty("code")
    public void setCode(String code) {
        if (this.linkCode == null) {
            this.linkCode = code;
        }
    }
}

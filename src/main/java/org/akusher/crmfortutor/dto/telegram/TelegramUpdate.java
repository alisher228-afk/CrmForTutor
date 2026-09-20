package org.akusher.crmfortutor.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramUpdate {

    @JsonProperty("update_id")
    private Long updateId;

    private TelegramMessage message;

    @JsonProperty("edited_message")
    private TelegramMessage editedMessage;

    public TelegramMessage getEffectiveMessage() {
        return message != null ? message : editedMessage;
    }
}

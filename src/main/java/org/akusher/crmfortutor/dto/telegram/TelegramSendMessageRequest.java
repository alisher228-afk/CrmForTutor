package org.akusher.crmfortutor.dto.telegram;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelegramSendMessageRequest {

    @JsonProperty("chat_id")
    private Long chatId;

    private String text;

    @JsonProperty("parse_mode")
    private String parseMode;
}

package org.akusher.crmfortutor.dto.telegram;

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
public class TelegramWebAppData {
    private TelegramWebAppUser user;
    private Instant authDate;
    private String queryId;
    private String startParam;
}

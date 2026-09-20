package org.akusher.crmfortutor.controller;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.akusher.crmfortutor.config.TelegramProperties;
import org.akusher.crmfortutor.dto.telegram.TelegramUpdate;
import org.akusher.crmfortutor.service.TelegramService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
@RestController
@RequestMapping("/api/v1/telegram")
@RequiredArgsConstructor
public class TelegramWebhookController {

    public static final String SECRET_TOKEN_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final TelegramService telegramService;
    private final TelegramProperties telegramProperties;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(telegramProperties.getWebhookSecretToken())) {
            log.warn("Telegram webhook secret token is not configured. Webhook endpoint is currently not protected!");
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = SECRET_TOKEN_HEADER, required = false) String secretToken,
            @RequestBody(required = false) TelegramUpdate update) {

        String expectedSecret = telegramProperties.getWebhookSecretToken();
        if (StringUtils.hasText(expectedSecret)) {
            if (secretToken == null || !MessageDigest.isEqual(
                    secretToken.getBytes(StandardCharsets.UTF_8),
                    expectedSecret.getBytes(StandardCharsets.UTF_8))) {
                log.warn("Unauthorized Telegram webhook request: secret token mismatch or missing");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        if (update != null) {
            log.info("Received Telegram webhook update id: {}", update.getUpdateId());
            telegramService.processUpdate(update);
        }
        return ResponseEntity.ok().build();
    }
}

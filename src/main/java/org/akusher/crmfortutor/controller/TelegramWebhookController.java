package org.akusher.crmfortutor.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.akusher.crmfortutor.dto.telegram.TelegramUpdate;
import org.akusher.crmfortutor.service.TelegramService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/telegram")
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final TelegramService telegramService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody(required = false) TelegramUpdate update) {
        if (update != null) {
            log.info("Received Telegram webhook update id: {}", update.getUpdateId());
            telegramService.processUpdate(update);
        }
        return ResponseEntity.ok().build();
    }
}

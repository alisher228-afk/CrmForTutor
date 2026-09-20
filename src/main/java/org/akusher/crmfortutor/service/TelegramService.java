package org.akusher.crmfortutor.service;

import lombok.extern.slf4j.Slf4j;
import org.akusher.crmfortutor.config.TelegramProperties;
import org.akusher.crmfortutor.dto.telegram.TelegramMessage;
import org.akusher.crmfortutor.dto.telegram.TelegramSendMessageRequest;
import org.akusher.crmfortutor.dto.telegram.TelegramUpdate;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.repository.StudentProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class TelegramService {

    private final StudentProfileRepository studentProfileRepository;
    private final TelegramProperties telegramProperties;
    private final RestClient restClient;

    @Autowired
    public TelegramService(
            StudentProfileRepository studentProfileRepository,
            TelegramProperties telegramProperties,
            @Autowired(required = false) RestClient.Builder restClientBuilder) {
        this.studentProfileRepository = studentProfileRepository;
        this.telegramProperties = telegramProperties;
        this.restClient = restClientBuilder != null ? restClientBuilder.build() : RestClient.create();
    }

    public TelegramService(
            StudentProfileRepository studentProfileRepository,
            TelegramProperties telegramProperties,
            RestClient restClient) {
        this.studentProfileRepository = studentProfileRepository;
        this.telegramProperties = telegramProperties;
        this.restClient = restClient != null ? restClient : RestClient.create();
    }

    public void sendMessage(Long chatId, String text) {
        if (!StringUtils.hasText(telegramProperties.getBotToken())) {
            log.warn("Telegram bot token is not configured. Skipping sendMessage to chatId: {}", chatId);
            return;
        }

        String url = String.format("%s/bot%s/sendMessage",
                telegramProperties.getApiUrl(),
                telegramProperties.getBotToken());

        TelegramSendMessageRequest request = TelegramSendMessageRequest.builder()
                .chatId(chatId)
                .text(text)
                .build();

        try {
            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sent Telegram message to chatId: {}", chatId);
        } catch (Exception e) {
            log.error("Failed to send Telegram message to chatId {}: {}", chatId, e.getMessage(), e);
        }
    }

    @Transactional
    public boolean processUpdate(TelegramUpdate update) {
        if (update == null) {
            return false;
        }

        TelegramMessage message = update.getEffectiveMessage();
        if (message == null || message.getChat() == null || !StringUtils.hasText(message.getText())) {
            return false;
        }

        Long chatId = message.getChat().getId();
        String rawText = message.getText().trim();
        String code = extractCode(rawText);

        if (StringUtils.hasText(code)) {
            Optional<StudentProfile> studentOpt = studentProfileRepository.findByTelegramLinkCode(code);
            if (studentOpt.isPresent()) {
                StudentProfile student = studentOpt.get();
                Instant expiresAt = student.getTelegramLinkCodeExpiresAt();
                if (expiresAt != null && expiresAt.isAfter(Instant.now())) {
                    student.setTelegramChatId(chatId);
                    student.setTelegramLinkCode(null);
                    student.setTelegramLinkCodeExpiresAt(null);
                    studentProfileRepository.save(student);

                    sendMessage(chatId, "Telegram успешно привязан к вашему профилю ученика! Теперь вы будете получать уведомления.");
                    return true;
                } else {
                    sendMessage(chatId, "Срок действия кода привязки истёк. Пожалуйста, запросите новый код у репетитора.");
                    return false;
                }
            }
        }

        if (rawText.startsWith("/start")) {
            sendMessage(chatId, "Здравствуйте! Для привязки Telegram отправьте 6-значный код привязки, полученный от вашего репетитора.");
        }

        return false;
    }

    private String extractCode(String rawText) {
        if (rawText.startsWith("/start")) {
            int spaceIndex = rawText.indexOf(' ');
            if (spaceIndex != -1) {
                return rawText.substring(spaceIndex + 1).trim();
            }
            return "";
        }
        return rawText;
    }
}

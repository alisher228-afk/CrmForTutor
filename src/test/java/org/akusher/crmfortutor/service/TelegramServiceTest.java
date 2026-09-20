package org.akusher.crmfortutor.service;

import org.akusher.crmfortutor.config.TelegramProperties;
import org.akusher.crmfortutor.dto.telegram.TelegramChat;
import org.akusher.crmfortutor.dto.telegram.TelegramMessage;
import org.akusher.crmfortutor.dto.telegram.TelegramUpdate;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.repository.StudentProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramServiceTest {

    @Mock
    private StudentProfileRepository studentProfileRepository;

    private TelegramProperties telegramProperties;
    private TelegramService telegramService;

    @BeforeEach
    void setUp() {
        telegramProperties = new TelegramProperties();
        telegramProperties.setBotToken("test-token");
        telegramProperties.setBotUsername("TestBot");
        telegramProperties.setApiUrl("https://api.telegram.org");

        // Use no-op or default RestClient for unit tests where external HTTP is avoided
        telegramService = new TelegramService(studentProfileRepository, telegramProperties, RestClient.create());
    }

    @Test
    @DisplayName("processUpdate - successfully links active code")
    void processUpdate_ValidCode_Success() {
        String code = "123456";
        Long chatId = 987654321L;

        StudentProfile student = StudentProfile.builder()
                .id(1L)
                .telegramLinkCode(code)
                .telegramLinkCodeExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();

        when(studentProfileRepository.findByTelegramLinkCode(code)).thenReturn(Optional.of(student));

        TelegramUpdate update = TelegramUpdate.builder()
                .updateId(101L)
                .message(TelegramMessage.builder()
                        .messageId(1L)
                        .chat(TelegramChat.builder().id(chatId).type("private").build())
                        .text("123456")
                        .build())
                .build();

        boolean result = telegramService.processUpdate(update);

        assertThat(result).isTrue();
        assertThat(student.getTelegramChatId()).isEqualTo(chatId);
        assertThat(student.getTelegramLinkCode()).isNull();
        assertThat(student.getTelegramLinkCodeExpiresAt()).isNull();
        verify(studentProfileRepository).save(student);
    }

    @Test
    @DisplayName("processUpdate - successfully links code from /start 123456 command")
    void processUpdate_StartWithCode_Success() {
        String code = "123456";
        Long chatId = 987654321L;

        StudentProfile student = StudentProfile.builder()
                .id(1L)
                .telegramLinkCode(code)
                .telegramLinkCodeExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();

        when(studentProfileRepository.findByTelegramLinkCode(code)).thenReturn(Optional.of(student));

        TelegramUpdate update = TelegramUpdate.builder()
                .updateId(102L)
                .message(TelegramMessage.builder()
                        .messageId(2L)
                        .chat(TelegramChat.builder().id(chatId).type("private").build())
                        .text("/start 123456")
                        .build())
                .build();

        boolean result = telegramService.processUpdate(update);

        assertThat(result).isTrue();
        assertThat(student.getTelegramChatId()).isEqualTo(chatId);
        assertThat(student.getTelegramLinkCode()).isNull();
        assertThat(student.getTelegramLinkCodeExpiresAt()).isNull();
        verify(studentProfileRepository).save(student);
    }

    @Test
    @DisplayName("processUpdate - expired code is rejected")
    void processUpdate_ExpiredCode_Rejected() {
        String code = "123456";
        Long chatId = 987654321L;

        StudentProfile student = StudentProfile.builder()
                .id(1L)
                .telegramLinkCode(code)
                .telegramLinkCodeExpiresAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .build();

        when(studentProfileRepository.findByTelegramLinkCode(code)).thenReturn(Optional.of(student));

        TelegramUpdate update = TelegramUpdate.builder()
                .updateId(103L)
                .message(TelegramMessage.builder()
                        .messageId(3L)
                        .chat(TelegramChat.builder().id(chatId).type("private").build())
                        .text("123456")
                        .build())
                .build();

        boolean result = telegramService.processUpdate(update);

        assertThat(result).isFalse();
        assertThat(student.getTelegramChatId()).isNull();
        verify(studentProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("processUpdate - unknown code is not linked")
    void processUpdate_UnknownCode() {
        when(studentProfileRepository.findByTelegramLinkCode("999999")).thenReturn(Optional.empty());

        TelegramUpdate update = TelegramUpdate.builder()
                .updateId(104L)
                .message(TelegramMessage.builder()
                        .messageId(4L)
                        .chat(TelegramChat.builder().id(12345L).type("private").build())
                        .text("999999")
                        .build())
                .build();

        boolean result = telegramService.processUpdate(update);

        assertThat(result).isFalse();
        verify(studentProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("processUpdate - plain /start command does not link")
    void processUpdate_StartWithoutCode() {
        TelegramUpdate update = TelegramUpdate.builder()
                .updateId(105L)
                .message(TelegramMessage.builder()
                        .messageId(5L)
                        .chat(TelegramChat.builder().id(12345L).type("private").build())
                        .text("/start")
                        .build())
                .build();

        boolean result = telegramService.processUpdate(update);

        assertThat(result).isFalse();
        verify(studentProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("processUpdate - null or invalid payload returns false safely")
    void processUpdate_NullPayload() {
        assertThat(telegramService.processUpdate(null)).isFalse();

        TelegramUpdate emptyUpdate = TelegramUpdate.builder().build();
        assertThat(telegramService.processUpdate(emptyUpdate)).isFalse();

        TelegramUpdate noTextUpdate = TelegramUpdate.builder()
                .message(TelegramMessage.builder()
                        .chat(TelegramChat.builder().id(123L).build())
                        .build())
                .build();
        assertThat(telegramService.processUpdate(noTextUpdate)).isFalse();
    }

    @Test
    @DisplayName("sendMessage - blank token skips without error")
    void sendMessage_BlankToken_Skips() {
        telegramProperties.setBotToken("");
        telegramService.sendMessage(12345L, "Test message");
        // No exception thrown
    }
}

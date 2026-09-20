package org.akusher.crmfortutor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.akusher.crmfortutor.config.TelegramProperties;
import org.akusher.crmfortutor.dto.telegram.TelegramChat;
import org.akusher.crmfortutor.dto.telegram.TelegramMessage;
import org.akusher.crmfortutor.dto.telegram.TelegramUpdate;
import org.akusher.crmfortutor.service.TelegramService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.akusher.crmfortutor.controller.TelegramWebhookController.SECRET_TOKEN_HEADER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TelegramWebhookControllerTest {

    private static final String SECRET_TOKEN = "secret-token-12345";

    private MockMvc mockMvc;

    @Mock
    private TelegramService telegramService;

    @Mock
    private TelegramProperties telegramProperties;

    @InjectMocks
    private TelegramWebhookController telegramWebhookController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(telegramWebhookController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/v1/telegram/webhook - 200 OK with valid secret token and valid update")
    void handleWebhook_Success_WithValidSecretToken() throws Exception {
        when(telegramProperties.getWebhookSecretToken()).thenReturn(SECRET_TOKEN);

        TelegramUpdate update = TelegramUpdate.builder()
                .updateId(12345L)
                .message(TelegramMessage.builder()
                        .messageId(1L)
                        .chat(TelegramChat.builder().id(999L).build())
                        .text("654321")
                        .build())
                .build();

        mockMvc.perform(post("/api/v1/telegram/webhook")
                        .header(SECRET_TOKEN_HEADER, SECRET_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());

        verify(telegramService).processUpdate(any(TelegramUpdate.class));
    }

    @Test
    @DisplayName("POST /api/v1/telegram/webhook - 401 Unauthorized when secret token header is missing")
    void handleWebhook_MissingSecretToken_Returns401() throws Exception {
        when(telegramProperties.getWebhookSecretToken()).thenReturn(SECRET_TOKEN);

        TelegramUpdate update = TelegramUpdate.builder()
                .updateId(12345L)
                .message(TelegramMessage.builder()
                        .messageId(1L)
                        .chat(TelegramChat.builder().id(999L).build())
                        .text("654321")
                        .build())
                .build();

        mockMvc.perform(post("/api/v1/telegram/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isUnauthorized());

        verify(telegramService, never()).processUpdate(any());
    }

    @Test
    @DisplayName("POST /api/v1/telegram/webhook - 401 Unauthorized when secret token header is incorrect")
    void handleWebhook_InvalidSecretToken_Returns401() throws Exception {
        when(telegramProperties.getWebhookSecretToken()).thenReturn(SECRET_TOKEN);

        TelegramUpdate update = TelegramUpdate.builder()
                .updateId(12345L)
                .build();

        mockMvc.perform(post("/api/v1/telegram/webhook")
                        .header(SECRET_TOKEN_HEADER, "wrong-secret-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isUnauthorized());

        verify(telegramService, never()).processUpdate(any());
    }

    @Test
    @DisplayName("POST /api/v1/telegram/webhook - 200 OK when secret token is not configured")
    void handleWebhook_SecretTokenNotConfigured_AllowsRequest() throws Exception {
        when(telegramProperties.getWebhookSecretToken()).thenReturn("");

        TelegramUpdate update = TelegramUpdate.builder()
                .updateId(12345L)
                .message(TelegramMessage.builder()
                        .messageId(1L)
                        .chat(TelegramChat.builder().id(999L).build())
                        .text("654321")
                        .build())
                .build();

        mockMvc.perform(post("/api/v1/telegram/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());

        verify(telegramService).processUpdate(any(TelegramUpdate.class));
    }

    @Test
    @DisplayName("POST /api/v1/telegram/webhook - 200 OK even without body when secret is valid")
    void handleWebhook_EmptyBody_WithValidSecret() throws Exception {
        when(telegramProperties.getWebhookSecretToken()).thenReturn(SECRET_TOKEN);

        mockMvc.perform(post("/api/v1/telegram/webhook")
                        .header(SECRET_TOKEN_HEADER, SECRET_TOKEN))
                .andExpect(status().isOk());

        verify(telegramService, never()).processUpdate(any());
    }

    @Test
    @DisplayName("init logs warning when secret token is empty or null")
    void init_WhenSecretTokenEmpty_DoesNotThrow() {
        when(telegramProperties.getWebhookSecretToken()).thenReturn(null);
        telegramWebhookController.init();

        when(telegramProperties.getWebhookSecretToken()).thenReturn("");
        telegramWebhookController.init();
    }
}

package org.akusher.crmfortutor.controller;

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
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TelegramWebhookControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TelegramService telegramService;

    @InjectMocks
    private TelegramWebhookController telegramWebhookController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(telegramWebhookController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/v1/telegram/webhook - 200 OK with valid update")
    void handleWebhook_Success() throws Exception {
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
    @DisplayName("POST /api/v1/telegram/webhook - 200 OK even without body")
    void handleWebhook_EmptyBody() throws Exception {
        mockMvc.perform(post("/api/v1/telegram/webhook"))
                .andExpect(status().isOk());
    }
}

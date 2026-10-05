package org.akusher.crmfortutor.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.akusher.crmfortutor.config.TelegramProperties;
import org.akusher.crmfortutor.dto.telegram.TelegramWebAppData;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelegramWebAppValidatorTest {

    private static final String BOT_TOKEN = "123456789:ABCdefGHIjklMNOpqrSTUvwxYZ123456789";

    private TelegramProperties telegramProperties;
    private ObjectMapper objectMapper;
    private TelegramWebAppValidator validator;

    @BeforeEach
    void setUp() {
        telegramProperties = new TelegramProperties();
        telegramProperties.setBotToken(BOT_TOKEN);
        objectMapper = new ObjectMapper();
        validator = new TelegramWebAppValidator(telegramProperties, objectMapper);
    }

    private String calculateTelegramHash(String botToken, String dataCheckString) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] secretKey = mac.doFinal(botToken.getBytes(StandardCharsets.UTF_8));

        mac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
        byte[] hashBytes = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashBytes);
    }

    @Test
    @DisplayName("validate - success with valid signature")
    void validate_Success() throws Exception {
        long authDate = Instant.now().getEpochSecond();
        String userJson = "{\"id\":987654,\"first_name\":\"Ivan\",\"last_name\":\"Petrov\",\"username\":\"ivan_p\"}";

        // Sorted keys: auth_date, query_id, user
        String dataCheckString = "auth_date=" + authDate + "\nquery_id=AAHd123\nuser=" + userJson;
        String hash = calculateTelegramHash(BOT_TOKEN, dataCheckString);

        String initData = "query_id=AAHd123&user=" + URLEncoder.encode(userJson, StandardCharsets.UTF_8)
                + "&auth_date=" + authDate + "&hash=" + hash;

        TelegramWebAppData result = validator.validate(initData);

        assertThat(result).isNotNull();
        assertThat(result.getUser()).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(987654L);
        assertThat(result.getUser().getFirstName()).isEqualTo("Ivan");
        assertThat(result.getUser().getLastName()).isEqualTo("Petrov");
        assertThat(result.getUser().getUsername()).isEqualTo("ivan_p");
        assertThat(result.getQueryId()).isEqualTo("AAHd123");
    }

    @Test
    @DisplayName("validate - throws BadRequestException on invalid signature")
    void validate_InvalidSignature_ThrowsException() {
        long authDate = Instant.now().getEpochSecond();
        String userJson = "{\"id\":987654,\"first_name\":\"Ivan\"}";
        String initData = "query_id=AAHd123&user=" + URLEncoder.encode(userJson, StandardCharsets.UTF_8)
                + "&auth_date=" + authDate + "&hash=wronghash123456789";

        assertThatThrownBy(() -> validator.validate(initData))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Недействительная подпись данных Telegram");
    }

    @Test
    @DisplayName("validate - throws BadRequestException when initData is empty")
    void validate_EmptyInitData_ThrowsException() {
        assertThatThrownBy(() -> validator.validate(""))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Параметр initData не должен быть пустым");
    }
}

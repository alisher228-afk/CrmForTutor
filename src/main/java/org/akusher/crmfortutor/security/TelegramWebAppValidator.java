package org.akusher.crmfortutor.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.akusher.crmfortutor.config.TelegramProperties;
import org.akusher.crmfortutor.dto.telegram.TelegramWebAppData;
import org.akusher.crmfortutor.dto.telegram.TelegramWebAppUser;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Component
public class TelegramWebAppValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String WEB_APP_DATA_KEY = "WebAppData";
    private static final long MAX_AUTH_AGE_SECONDS = 86400L; // 24 hours

    private final TelegramProperties telegramProperties;
    private final ObjectMapper objectMapper;

    @Autowired
    public TelegramWebAppValidator(
            TelegramProperties telegramProperties,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this.telegramProperties = telegramProperties;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Validates Telegram WebApp initData string and returns parsed data.
     *
     * @param initData raw query string from window.Telegram.WebApp.initData
     * @return validated TelegramWebAppData
     */
    public TelegramWebAppData validate(String initData) {
        if (!StringUtils.hasText(initData)) {
            throw new BadRequestException("Параметр initData не должен быть пустым");
        }

        String botToken = telegramProperties.getBotToken();
        if (!StringUtils.hasText(botToken)) {
            throw new BadRequestException("Telegram bot token не настроен на сервере");
        }

        Map<String, String> params = parseInitData(initData);
        String hash = params.remove("hash");
        if (!StringUtils.hasText(hash)) {
            throw new BadRequestException("Хэш подписи Telegram отсутствует в initData");
        }

        // Build data_check_string
        StringBuilder dataCheckStringBuilder = new StringBuilder();
        int index = 0;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (index > 0) {
                dataCheckStringBuilder.append("\n");
            }
            dataCheckStringBuilder.append(entry.getKey()).append("=").append(entry.getValue());
            index++;
        }
        String dataCheckString = dataCheckStringBuilder.toString();

        // Calculate and verify signature
        if (!verifySignature(botToken, dataCheckString, hash)) {
            log.warn("Telegram WebApp signature verification failed");
            throw new BadRequestException("Недействительная подпись данных Telegram");
        }

        // Check auth_date
        Instant authDate = null;
        String authDateStr = params.get("auth_date");
        if (StringUtils.hasText(authDateStr)) {
            try {
                long authSeconds = Long.parseLong(authDateStr);
                authDate = Instant.ofEpochSecond(authSeconds);
                long age = Math.abs(Duration.between(authDate, Instant.now()).getSeconds());
                if (age > MAX_AUTH_AGE_SECONDS) {
                    log.warn("Telegram WebApp initData expired, age: {} seconds", age);
                    throw new BadRequestException("Срок действия данных сессии Telegram истёк");
                }
            } catch (NumberFormatException e) {
                throw new BadRequestException("Некорректный формат auth_date в Telegram initData");
            }
        }

        // Parse user
        String userJson = params.get("user");
        if (!StringUtils.hasText(userJson)) {
            throw new BadRequestException("Информация о пользователе отсутствует в Telegram initData");
        }

        TelegramWebAppUser user;
        try {
            user = objectMapper.readValue(userJson, TelegramWebAppUser.class);
        } catch (Exception e) {
            log.error("Failed to parse Telegram user JSON: {}", userJson, e);
            throw new BadRequestException("Ошибка разбора профиля пользователя Telegram");
        }

        return TelegramWebAppData.builder()
                .user(user)
                .authDate(authDate)
                .queryId(params.get("query_id"))
                .startParam(params.get("start_param"))
                .build();
    }

    private boolean verifySignature(String botToken, String dataCheckString, String expectedHash) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(WEB_APP_DATA_KEY.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] secretKey = mac.doFinal(botToken.getBytes(StandardCharsets.UTF_8));

            mac.init(new SecretKeySpec(secretKey, HMAC_SHA256));
            byte[] calculatedHashBytes = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
            String calculatedHashHex = HexFormat.of().formatHex(calculatedHashBytes);

            return MessageDigest.isEqual(
                    calculatedHashHex.getBytes(StandardCharsets.UTF_8),
                    expectedHash.toLowerCase().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Error computing HMAC-SHA256 for Telegram WebApp data", e);
            return false;
        }
    }

    private Map<String, String> parseInitData(String initData) {
        Map<String, String> map = new TreeMap<>();
        String[] pairs = initData.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) {
                continue;
            }
            int eqIdx = pair.indexOf('=');
            if (eqIdx != -1) {
                String key = URLDecoder.decode(pair.substring(0, eqIdx), StandardCharsets.UTF_8);
                String val = URLDecoder.decode(pair.substring(eqIdx + 1), StandardCharsets.UTF_8);
                map.put(key, val);
            }
        }
        return map;
    }
}

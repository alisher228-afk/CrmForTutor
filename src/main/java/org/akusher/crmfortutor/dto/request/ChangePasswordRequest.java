package org.akusher.crmfortutor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class ChangePasswordRequest {

    @NotBlank(message = "Текущий пароль обязателен")
    @Size(min = 6, max = 100, message = "Пароль должен содержать от 6 до 100 символов")
    private String currentPassword;

    @NotBlank(message = "Новый пароль обязателен")
    @Size(min = 6, max = 100, message = "Новый пароль должен содержать от 6 до 100 символов")
    private String newPassword;
}

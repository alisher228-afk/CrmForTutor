package org.akusher.crmfortutor.dto.request;

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
public class UserProfileUpdateRequest {

    @Size(max = 100, message = "Имя не должно превышать 100 символов")
    private String firstName;

    @Size(max = 100, message = "Фамилия не должна превышать 100 символов")
    private String lastName;

    @Size(max = 50, message = "Телефон не должен превышать 50 символов")
    private String phone;

    @Size(max = 255, message = "Специализация не должна превышать 255 символов")
    private String specialization;
}

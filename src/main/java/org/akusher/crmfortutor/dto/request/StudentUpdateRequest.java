package org.akusher.crmfortutor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.akusher.crmfortutor.entity.StudentStatus;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentUpdateRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Size(max = 50, message = "Phone must not exceed 50 characters")
    private String phone;

    @Size(max = 100, message = "Telegram username must not exceed 100 characters")
    private String telegram;

    @Size(max = 100, message = "Current level must not exceed 100 characters")
    private String currentLevel;

    @PositiveOrZero(message = "Hourly rate must be greater than or equal to 0")
    private BigDecimal hourlyRate;

    private String notes;

    @NotNull(message = "Status is required")
    private StudentStatus status;
}

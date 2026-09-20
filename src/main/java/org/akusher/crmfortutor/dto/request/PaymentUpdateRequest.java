package org.akusher.crmfortutor.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentUpdateRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Lessons count is required")
    @Positive(message = "Lessons count must be positive")
    private Integer lessonsCount;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}

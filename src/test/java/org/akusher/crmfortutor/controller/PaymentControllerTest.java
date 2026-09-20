package org.akusher.crmfortutor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.akusher.crmfortutor.dto.request.PaymentUpdateRequest;
import org.akusher.crmfortutor.dto.response.PaymentResponse;
import org.akusher.crmfortutor.exception.GlobalExceptionHandler;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.service.PaymentService;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("PUT /api/v1/payments/{id} - 200 OK")
    void updatePayment_Success() throws Exception {
        PaymentUpdateRequest request = PaymentUpdateRequest.builder()
                .amount(BigDecimal.valueOf(6000))
                .lessonsCount(6)
                .paymentDate(LocalDate.of(2026, 9, 20))
                .notes("Payment updated")
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .studentId(2L)
                .amount(BigDecimal.valueOf(6000))
                .lessonsCount(6)
                .paymentDate(LocalDate.of(2026, 9, 20))
                .notes("Payment updated")
                .build();

        when(paymentService.updatePayment(eq(1L), any(PaymentUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/payments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(6000))
                .andExpect(jsonPath("$.lessonsCount").value(6))
                .andExpect(jsonPath("$.notes").value("Payment updated"));
    }

    @Test
    @DisplayName("PUT /api/v1/payments/{id} - 400 Bad Request on invalid fields")
    void updatePayment_ValidationError() throws Exception {
        PaymentUpdateRequest request = PaymentUpdateRequest.builder()
                .amount(BigDecimal.valueOf(-100)) // invalid negative amount
                .lessonsCount(0) // invalid non-positive count
                .build();

        mockMvc.perform(put("/api/v1/payments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/v1/payments/{id} - 204 No Content")
    void deletePayment_Success() throws Exception {
        doNothing().when(paymentService).deletePayment(1L);

        mockMvc.perform(delete("/api/v1/payments/1"))
                .andExpect(status().isNoContent());

        verify(paymentService).deletePayment(1L);
    }

    @Test
    @DisplayName("DELETE /api/v1/payments/{id} - 404 Not Found")
    void deletePayment_NotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Payment not found with id: 99"))
                .when(paymentService).deletePayment(99L);

        mockMvc.perform(delete("/api/v1/payments/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Payment not found with id: 99"));
    }
}

package org.akusher.crmfortutor.mapper;

import org.akusher.crmfortutor.dto.response.PaymentResponse;
import org.akusher.crmfortutor.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "studentId", source = "student.id")
    PaymentResponse toResponse(Payment entity);

    List<PaymentResponse> toResponseList(List<Payment> entities);
}

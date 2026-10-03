package com.careconnect.dto;

import com.careconnect.entity.ClinicalOrder;
import com.careconnect.entity.OrderPriority;
import com.careconnect.entity.OrderStatus;
import com.careconnect.entity.OrderType;

import java.time.LocalDateTime;

public record ClinicalOrderResponse(
        Long id,
        Long patientId,
        Long doctorId,
        Long encounterId,
        OrderType orderType,
        String description,
        OrderPriority priority,
        OrderStatus status,
        LocalDateTime orderedAt,
        LocalDateTime completedAt
) {
    public static ClinicalOrderResponse fromEntity(ClinicalOrder order) {
        return new ClinicalOrderResponse(
                order.getId(),
                order.getPatientId(),
                order.getDoctorId(),
                order.getEncounterId(),
                order.getOrderType(),
                order.getDescription(),
                order.getPriority(),
                order.getStatus(),
                order.getOrderedAt(),
                order.getCompletedAt()
        );
    }
}

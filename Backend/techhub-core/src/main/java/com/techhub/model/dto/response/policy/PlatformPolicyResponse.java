package com.techhub.model.dto.response.policy;

import com.techhub.model.entity.TransactionPolicy;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record PlatformPolicyResponse(
        UUID id,
        String policyKey,
        String policyValue,
        String description,
        UUID updatedById,
        String updatedByName,
        LocalDateTime updatedAt,
        LocalDateTime createdAt
) {
    public static PlatformPolicyResponse from(TransactionPolicy policy) {
        String adminName = null;
        UUID adminId = null;
        if (policy.getUpdatedBy() != null) {
            adminId = policy.getUpdatedBy().getId();
            String firstName = policy.getUpdatedBy().getFirstName();
            String lastName = policy.getUpdatedBy().getLastName();
            if (firstName != null && !firstName.isBlank()) {
                adminName = (lastName != null && !lastName.isBlank()) ? lastName + " " + firstName : firstName;
            } else {
                adminName = policy.getUpdatedBy().getEmail();
            }
        }

        return PlatformPolicyResponse.builder()
                .id(policy.getId())
                .policyKey(policy.getPolicyKey())
                .policyValue(policy.getPolicyValue())
                .description(policy.getDescription())
                .updatedById(adminId)
                .updatedByName(adminName)
                .updatedAt(policy.getUpdatedAt())
                .createdAt(policy.getCreatedAt())
                .build();
    }
}

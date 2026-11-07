package com.techhub.model.dto.request.policy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePlatformPolicyRequest(
        @NotBlank(message = "Policy key must not be blank")
        @Size(max = 100, message = "Policy key must not exceed 100 characters")
        String policyKey,

        @NotBlank(message = "Policy value must not be blank")
        @Size(max = 255, message = "Policy value must not exceed 255 characters")
        String policyValue,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description
) {
}

package com.techhub.service.policy;

import com.techhub.model.dto.request.policy.UpdatePlatformPolicyRequest;
import com.techhub.model.dto.response.policy.PlatformPolicyResponse;

import java.util.List;
import java.util.UUID;

public interface AdminPolicyService {

    List<PlatformPolicyResponse> getAllPolicies();

    PlatformPolicyResponse updatePolicy(UUID adminId, UpdatePlatformPolicyRequest request);
}

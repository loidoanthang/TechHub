package com.techhub.service.policy;

import com.techhub.modules.policy.dto.request.UpdatePlatformPolicyRequest;
import com.techhub.modules.policy.dto.response.PlatformPolicyResponse;

import java.util.List;
import java.util.UUID;

public interface AdminPolicyService {

    List<PlatformPolicyResponse> getAllPolicies();

    PlatformPolicyResponse updatePolicy(UUID adminId, UpdatePlatformPolicyRequest request);
}

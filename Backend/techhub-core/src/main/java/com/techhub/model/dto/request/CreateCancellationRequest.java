package com.techhub.model.dto.request;

import com.techhub.model.enums.CancellationReasonCode;
import jakarta.validation.constraints.Size;

public record CreateCancellationRequest(
        @Size(max = 500, message = "Lý do hủy đơn không được vượt quá 500 ký tự")
        String reason,

        CancellationReasonCode reasonCode,

        @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
        String reasonNote
) {
    public CreateCancellationRequest(String reason) {
        this(reason, null, null);
    }
}

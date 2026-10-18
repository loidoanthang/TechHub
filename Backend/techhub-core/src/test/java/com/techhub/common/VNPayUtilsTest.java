package com.techhub.common;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class VNPayUtilsTest {

    private static final String SECRET_KEY = "DHPMLPLWZVHLGFDDDIZKQFFRIMHDDBWW";

    @Test
    @DisplayName("hmacSHA512 should compute correct hash for known vector")
    void testHmacSHA512_Success() {
        String data = "test_data";
        String hash = VNPayUtils.hmacSHA512(SECRET_KEY, data);

        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(128); // SHA-512 hex is 128 characters
        assertThat(hash).isEqualTo("95f9747beb4ec63c4cbf855924d3482288e31812386c416652ba4b0cc11001a18cf04097252b4bd5a19a3081e0e1f7812107510261d8c516942bb8bade218174");
    }

    @Test
    @DisplayName("hmacSHA512 should throw exception on null input")
    void testHmacSHA512_NullInput() {
        assertThatThrownBy(() -> VNPayUtils.hmacSHA512(null, "data"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> VNPayUtils.hmacSHA512(SECRET_KEY, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("hashAllFields should sort fields, filter secure hash keys and return signature")
    void testHashAllFields() {
        Map<String, String> fields = new HashMap<>();
        fields.put("vnp_TxnRef", "ORDER_1001");
        fields.put("vnp_Amount", "10000000");
        fields.put("vnp_Command", "pay");
        fields.put("vnp_SecureHash", "IGNORED_HASH");
        fields.put("vnp_SecureHashType", "SHA512");
        fields.put("empty_field", "");
        fields.put("null_field", null);

        String hash = VNPayUtils.hashAllFields(fields, SECRET_KEY);
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(128);

        // Expected hash is computed over: vnp_Amount=10000000&vnp_Command=pay&vnp_TxnRef=ORDER_1001
        String expectedData = "vnp_Amount=10000000&vnp_Command=pay&vnp_TxnRef=ORDER_1001";
        String expectedHash = VNPayUtils.hmacSHA512(SECRET_KEY, expectedData);
        assertThat(hash).isEqualTo(expectedHash);
    }

    @Test
    @DisplayName("hashAllFields should return empty string if fields or secret is empty")
    void testHashAllFields_Empty() {
        assertThat(VNPayUtils.hashAllFields(null, SECRET_KEY)).isEmpty();
        assertThat(VNPayUtils.hashAllFields(new HashMap<>(), SECRET_KEY)).isEmpty();
        assertThat(VNPayUtils.hashAllFields(Map.of("key", "val"), null)).isEmpty();
        assertThat(VNPayUtils.hashAllFields(Map.of("key", "val"), "  ")).isEmpty();
    }

    @Test
    @DisplayName("buildQueryString should sort and encode parameters correctly")
    void testBuildQueryString() {
        Map<String, String> fields = new HashMap<>();
        fields.put("vnp_TxnRef", "ORDER 1001");
        fields.put("vnp_Amount", "500000");
        fields.put("vnp_SecureHash", "should_not_be_included");
        fields.put("empty", "");

        String queryString = VNPayUtils.buildQueryString(fields);
        assertThat(queryString).isEqualTo("vnp_Amount=500000&vnp_TxnRef=ORDER+1001");
    }

    @Test
    @DisplayName("buildQueryString should return empty string when map is null or empty")
    void testBuildQueryString_Empty() {
        assertThat(VNPayUtils.buildQueryString(null)).isEmpty();
        assertThat(VNPayUtils.buildQueryString(Map.of())).isEmpty();
    }

    @Test
    @DisplayName("verifySignature should return true for valid hash and false for tampered data")
    void testVerifySignature() {
        Map<String, String> fields = new HashMap<>();
        fields.put("vnp_TxnRef", "TXN_123");
        fields.put("vnp_Amount", "25000000");
        fields.put("vnp_ResponseCode", "00");

        String validHash = VNPayUtils.hashAllFields(fields, SECRET_KEY);
        fields.put("vnp_SecureHash", validHash);

        assertThat(VNPayUtils.verifySignature(fields, SECRET_KEY)).isTrue();

        // Tamper with amount
        fields.put("vnp_Amount", "99999999");
        assertThat(VNPayUtils.verifySignature(fields, SECRET_KEY)).isFalse();

        // Missing hash
        fields.remove("vnp_SecureHash");
        assertThat(VNPayUtils.verifySignature(fields, SECRET_KEY)).isFalse();

        // Null/empty secret
        assertThat(VNPayUtils.verifySignature(fields, null)).isFalse();
    }

    @Test
    @DisplayName("getClientIp should parse various headers correctly")
    void testGetClientIp() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        // Test X-Forwarded-For with multiple IPs
        when(request.getHeader("X-Forwarded-For")).thenReturn("103.20.10.5, 10.0.0.1");
        assertThat(VNPayUtils.getClientIp(request)).isEqualTo("103.20.10.5");

        // Test unknown header falls back to remoteAddr
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getRemoteAddr()).thenReturn("192.168.1.50");
        assertThat(VNPayUtils.getClientIp(request)).isEqualTo("192.168.1.50");

        // Test IPv6 localhost normalization
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");
        assertThat(VNPayUtils.getClientIp(request)).isEqualTo("127.0.0.1");

        when(request.getRemoteAddr()).thenReturn("::1");
        assertThat(VNPayUtils.getClientIp(request)).isEqualTo("127.0.0.1");

        // Test null request
        assertThat(VNPayUtils.getClientIp(null)).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("formatVNPayDate and parseVNPayDate should format and parse correctly")
    void testDateFormattingAndParsing() {
        LocalDateTime time = LocalDateTime.of(2026, 10, 18, 14, 30, 45);
        String formatted = VNPayUtils.formatVNPayDate(time);
        assertThat(formatted).isEqualTo("20261018143045");

        LocalDateTime parsed = VNPayUtils.parseVNPayDate(formatted);
        assertThat(parsed).isEqualTo(time);

        assertThat(VNPayUtils.formatVNPayDate(null)).isEmpty();
        assertThat(VNPayUtils.parseVNPayDate(null)).isNull();
        assertThat(VNPayUtils.parseVNPayDate("invalid")).isNull();

        assertThat(VNPayUtils.getCurrentTimeVN()).isNotNull();
        assertThat(VNPayUtils.getExpireTimeVN(15)).isAfter(VNPayUtils.getCurrentTimeVN().minusSeconds(5));
    }
}

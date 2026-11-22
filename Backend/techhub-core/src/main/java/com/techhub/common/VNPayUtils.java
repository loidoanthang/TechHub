package com.techhub.common;

import com.techhub.modules.payment.dto.response.VNPayQueryDrResponse;
import com.techhub.modules.payment.dto.response.VNPayRefundResponse;
import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class VNPayUtils {

    public static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String HMAC_SHA512 = "HmacSHA512";

    private VNPayUtils() {
    }

    /**
     * Compute HMAC-SHA512 hash for given data with secret key.
     *
     * @param key  VNPAY Hash Secret
     * @param data Data string to sign
     * @return Hex lowercase string of HMAC-SHA512 signature
     */
    public static String hmacSHA512(String key, String data) {
        if (key == null || data == null) {
            throw new IllegalArgumentException("Key and data must not be null for HMAC-SHA512 calculation");
        }
        try {
            Mac hmac512 = Mac.getInstance(HMAC_SHA512);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA512);
            hmac512.init(secretKey);
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * bytes.length);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA512", e);
        }
    }

    /**
     * Build hash data from query parameters and calculate HMAC-SHA512 checksum.
     * Follows VNPAY 2.1.0 specification:
     * - Filters out null/blank keys and values
     * - Excludes vnp_SecureHash and vnp_SecureHashType
     * - Sorts parameters in alphabetical order
     * - Encodes values with US_ASCII
     *
     * @param fields    Map of query parameters
     * @param secretKey VNPAY Hash Secret
     * @return Checksum hash string
     */
    public static String hashAllFields(Map<String, String> fields, String secretKey) {
        if (fields == null || fields.isEmpty() || secretKey == null || secretKey.isBlank()) {
            return "";
        }

        Map<String, String> sortedFields = new TreeMap<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                if (!"vnp_SecureHash".equalsIgnoreCase(key) && !"vnp_SecureHashType".equalsIgnoreCase(key)) {
                    sortedFields.put(key, value);
                }
            }
        }

        StringBuilder hashData = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : sortedFields.entrySet()) {
            if (!first) {
                hashData.append('&');
            }
            hashData.append(entry.getKey());
            hashData.append('=');
            hashData.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII));
            first = false;
        }

        return hmacSHA512(secretKey, hashData.toString());
    }

    /**
     * Build alphabetical sorted query string encoded with US_ASCII for VNPAY redirect URL.
     *
     * @param fields Map of query parameters
     * @return URL encoded query string
     */
    public static String buildQueryString(Map<String, String> fields) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }

        Map<String, String> sortedFields = new TreeMap<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                if (!"vnp_SecureHash".equalsIgnoreCase(key) && !"vnp_SecureHashType".equalsIgnoreCase(key)) {
                    sortedFields.put(key, value);
                }
            }
        }

        StringBuilder query = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : sortedFields.entrySet()) {
            if (!first) {
                query.append('&');
            }
            query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII));
            query.append('=');
            query.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII));
            first = false;
        }

        return query.toString();
    }

    /**
     * Verify VNPAY signature from IPN webhook or callback redirect params.
     *
     * @param fields    Map of query parameters including vnp_secure_hash or vnp_SecureHash
     * @param secretKey VNPAY Hash Secret
     * @return true if signature is valid, false otherwise
     */
    public static boolean verifySignature(Map<String, String> fields, String secretKey) {
        if (fields == null || fields.isEmpty() || secretKey == null || secretKey.isBlank()) {
            return false;
        }

        String secureHash = fields.get("vnp_secure_hash");
        if (secureHash == null || secureHash.isBlank()) {
            secureHash = fields.get("vnp_SecureHash");
        }
        if (secureHash == null || secureHash.isBlank()) {
            return false;
        }

        String calculatedHash = hashAllFields(fields, secretKey);
        boolean match = calculatedHash.equalsIgnoreCase(secureHash.trim());
        if (!match) {
            String rawHash = hashAllFieldsRaw(fields, secretKey);
            if (rawHash.equalsIgnoreCase(secureHash.trim())) {
                return true;
            }
            // Fallback: Check if vnp_txn_desc had '+' that got decoded to space or vice versa
            String altHash = hashAllFieldsAltSpaces(fields, secretKey);
            if (altHash.equalsIgnoreCase(secureHash.trim())) {
                return true;
            }
            org.slf4j.LoggerFactory.getLogger(VNPayUtils.class).warn(
                    "VNPay checksum mismatch! incomingHash={}, calculatedHash={}, rawHash={}, altHash={}",
                    secureHash, calculatedHash, rawHash, altHash
            );
        }
        return match;
    }

    /**
     * Fallback hash replacing spaces with '+' or vice versa in values
     */
    public static String hashAllFieldsAltSpaces(Map<String, String> fields, String secretKey) {
        if (fields == null || fields.isEmpty() || secretKey == null || secretKey.isBlank()) {
            return "";
        }

        Map<String, String> sortedFields = new TreeMap<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                if (!"vnp_SecureHash".equalsIgnoreCase(key) && !"vnp_SecureHashType".equalsIgnoreCase(key)) {
                    sortedFields.put(key, value.replace(" ", "+"));
                }
            }
        }

        StringBuilder hashData = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : sortedFields.entrySet()) {
            if (!first) {
                hashData.append('&');
            }
            hashData.append(entry.getKey());
            hashData.append('=');
            hashData.append(entry.getValue());
            first = false;
        }

        return hmacSHA512(secretKey, hashData.toString());
    }

    /**
     * Fallback hash without URLEncoder in case gateway signed unencoded values
     */
    public static String hashAllFieldsRaw(Map<String, String> fields, String secretKey) {
        if (fields == null || fields.isEmpty() || secretKey == null || secretKey.isBlank()) {
            return "";
        }

        Map<String, String> sortedFields = new TreeMap<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                if (!"vnp_SecureHash".equalsIgnoreCase(key) && !"vnp_SecureHashType".equalsIgnoreCase(key)) {
                    sortedFields.put(key, value);
                }
            }
        }

        StringBuilder hashData = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : sortedFields.entrySet()) {
            if (!first) {
                hashData.append('&');
            }
            hashData.append(entry.getKey());
            hashData.append('=');
            hashData.append(entry.getValue());
            first = false;
        }

        return hmacSHA512(secretKey, hashData.toString());
    }

    /**
     * Safely resolve client IPv4 address from HTTP request headers or remoteAddr.
     *
     * @param request HttpServletRequest
     * @return IPv4 address (default 127.0.0.1)
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }

        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return normalizeIp(ip);
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return normalizeIp(remoteAddr);
    }

    private static String normalizeIp(String ip) {
        if (ip == null || ip.isBlank() || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip.trim();
    }

    /**
     * Format a LocalDateTime to VNPAY standard string format (yyyyMMddHHmmss).
     */
    public static String formatVNPayDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATE_FORMATTER);
    }

    /**
     * Parse a VNPAY standard date string (yyyyMMddHHmmss) into LocalDateTime.
     */
    public static LocalDateTime parseVNPayDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || dateStr.length() != 14) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get current time in Vietnam timezone (GMT+7).
     */
    public static LocalDateTime getCurrentTimeVN() {
        return LocalDateTime.now(VN_ZONE);
    }

    /**
     * Get expire time (current time + minutes) in Vietnam timezone (GMT+7).
     */
    public static LocalDateTime getExpireTimeVN(int minutes) {
        return LocalDateTime.now(VN_ZONE).plusMinutes(minutes);
    }

    /**
     * Build HMAC-SHA512 checksum for VNPAY QueryDR request.
     * Follows pipe-separated order:
     * vnp_RequestId|vnp_Version|vnp_Command|vnp_TmnCode|vnp_TxnRef|vnp_TransactionDate|vnp_CreateDate|vnp_IpAddr|vnp_OrderInfo
     */
    public static String buildQueryDrChecksum(
            String requestId,
            String version,
            String command,
            String tmnCode,
            String txnRef,
            String transactionDate,
            String createDate,
            String ipAddr,
            String orderInfo,
            String secretKey
    ) {
        String rawData = String.join("|",
                Objects.requireNonNullElse(requestId, ""),
                Objects.requireNonNullElse(version, "2.1.0"),
                Objects.requireNonNullElse(command, "querydr"),
                Objects.requireNonNullElse(tmnCode, ""),
                Objects.requireNonNullElse(txnRef, ""),
                Objects.requireNonNullElse(transactionDate, ""),
                Objects.requireNonNullElse(createDate, ""),
                Objects.requireNonNullElse(ipAddr, ""),
                Objects.requireNonNullElse(orderInfo, "")
        );
        return hmacSHA512(secretKey, rawData);
    }

    /**
     * Build HMAC-SHA512 checksum for VNPAY Refund request.
     * Follows pipe-separated order:
     * vnp_RequestId|vnp_Version|vnp_Command|vnp_TmnCode|vnp_TransactionType|vnp_TxnRef|vnp_Amount|vnp_TransactionNo|vnp_TransactionDate|vnp_CreateBy|vnp_CreateDate|vnp_IpAddr|vnp_OrderInfo
     */
    public static String buildRefundChecksum(
            String requestId,
            String version,
            String command,
            String tmnCode,
            String transactionType,
            String txnRef,
            long amount,
            String transactionNo,
            String transactionDate,
            String createBy,
            String createDate,
            String ipAddr,
            String orderInfo,
            String secretKey
    ) {
        String rawData = String.join("|",
                Objects.requireNonNullElse(requestId, ""),
                Objects.requireNonNullElse(version, "2.1.0"),
                Objects.requireNonNullElse(command, "refund"),
                Objects.requireNonNullElse(tmnCode, ""),
                Objects.requireNonNullElse(transactionType, "02"),
                Objects.requireNonNullElse(txnRef, ""),
                String.valueOf(amount),
                Objects.requireNonNullElse(transactionNo, "0"),
                Objects.requireNonNullElse(transactionDate, ""),
                Objects.requireNonNullElse(createBy, ""),
                Objects.requireNonNullElse(createDate, ""),
                Objects.requireNonNullElse(ipAddr, ""),
                Objects.requireNonNullElse(orderInfo, "")
        );
        return hmacSHA512(secretKey, rawData);
    }

    /**
     * Verify HMAC-SHA512 checksum for VNPAY QueryDR response.
     * Response fields order:
     * vnp_ResponseId|vnp_Command|vnp_ResponseCode|vnp_Message|vnp_TmnCode|vnp_TxnRef|vnp_Amount|vnp_BankCode|vnp_PayDate|vnp_TransactionNo|vnp_TransactionType|vnp_TransactionStatus|vnp_OrderInfo|vnp_PromotionCode|vnp_PromotionAmount
     */
    public static boolean verifyQueryDrChecksum(VNPayQueryDrResponse response, String secretKey) {
        if (response == null || response.vnpSecureHash() == null || secretKey == null) {
            return false;
        }
        String rawData = String.join("|",
                Objects.requireNonNullElse(response.vnpResponseId(), ""),
                Objects.requireNonNullElse(response.vnpCommand(), ""),
                Objects.requireNonNullElse(response.vnpResponseCode(), ""),
                Objects.requireNonNullElse(response.vnpMessage(), ""),
                Objects.requireNonNullElse(response.vnpTmnCode(), ""),
                Objects.requireNonNullElse(response.vnpTxnRef(), ""),
                Objects.requireNonNullElse(response.vnpAmount(), ""),
                Objects.requireNonNullElse(response.vnpBankCode(), ""),
                Objects.requireNonNullElse(response.vnpPayDate(), ""),
                Objects.requireNonNullElse(response.vnpTransactionNo(), ""),
                Objects.requireNonNullElse(response.vnpTransactionType(), ""),
                Objects.requireNonNullElse(response.vnpTransactionStatus(), ""),
                Objects.requireNonNullElse(response.vnpOrderInfo(), ""),
                Objects.requireNonNullElse(response.vnpPromotionCode(), ""),
                Objects.requireNonNullElse(response.vnpPromotionAmount(), "")
        );
        String expectedHash = hmacSHA512(secretKey, rawData);
        return expectedHash.equalsIgnoreCase(response.vnpSecureHash().trim());
    }

    /**
     * Verify HMAC-SHA512 checksum for VNPAY Refund response.
     * Response fields order:
     * vnp_ResponseId|vnp_Command|vnp_ResponseCode|vnp_Message|vnp_TmnCode|vnp_TxnRef|vnp_Amount|vnp_BankCode|vnp_PayDate|vnp_TransactionNo|vnp_TransactionType|vnp_TransactionStatus|vnp_OrderInfo
     */
    public static boolean verifyRefundChecksum(VNPayRefundResponse response, String secretKey) {
        if (response == null || response.vnpSecureHash() == null || secretKey == null) {
            return false;
        }
        String rawData = String.join("|",
                Objects.requireNonNullElse(response.vnpResponseId(), ""),
                Objects.requireNonNullElse(response.vnpCommand(), ""),
                Objects.requireNonNullElse(response.vnpResponseCode(), ""),
                Objects.requireNonNullElse(response.vnpMessage(), ""),
                Objects.requireNonNullElse(response.vnpTmnCode(), ""),
                Objects.requireNonNullElse(response.vnpTxnRef(), ""),
                Objects.requireNonNullElse(response.vnpAmount(), ""),
                Objects.requireNonNullElse(response.vnpBankCode(), ""),
                Objects.requireNonNullElse(response.vnpPayDate(), ""),
                Objects.requireNonNullElse(response.vnpTransactionNo(), ""),
                Objects.requireNonNullElse(response.vnpTransactionType(), ""),
                Objects.requireNonNullElse(response.vnpTransactionStatus(), ""),
                Objects.requireNonNullElse(response.vnpOrderInfo(), "")
        );
        String expectedHash = hmacSHA512(secretKey, rawData);
        return expectedHash.equalsIgnoreCase(response.vnpSecureHash().trim());
    }
}

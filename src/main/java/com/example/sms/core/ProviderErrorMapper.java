package com.example.sms.core;

import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.aliyun.teautil.exception.TeaException;

/**
 * 服务商错误码到领域错误码的映射。
 */
@Component
public class ProviderErrorMapper {

    private static final Set<String> THROTTLED_CODES = Set.of(
        "isv.BUSINESS_LIMIT_CONTROL",
        "BUSINESS_LIMIT_CONTROL",
        "Throttling"
    );

    private static final Set<String> INVALID_PARAM_CODES = Set.of(
        "isv.INVALID_PARAMETERS",
        "isv.MISSING_PARAMETER",
        "MissingParameter",
        "InvalidParameter",
        "InvalidPhoneNumber",
        "SignatureNonceUsed"
    );

    private static final Set<String> AUTH_CODES = Set.of(
        "InvalidAccessKeyId.NotFound",
        "SignatureDoesNotMatch",
        "isv.ACCOUNT_NOT_EXISTS",
        "AuthFailure"
    );

    public ErrorCode mapCode(String providerCode) {
        if (!StringUtils.hasText(providerCode)) {
            return ErrorCode.PROVIDER_ERROR;
        }
        String code = providerCode.trim();
        if (THROTTLED_CODES.contains(code)) {
            return ErrorCode.PROVIDER_THROTTLED;
        }
        if (INVALID_PARAM_CODES.contains(code)) {
            return ErrorCode.PROVIDER_INVALID_PARAM;
        }
        if (AUTH_CODES.contains(code)) {
            return ErrorCode.PROVIDER_AUTH_ERROR;
        }
        return ErrorCode.PROVIDER_ERROR;
    }

    public ErrorCode mapException(Exception ex) {
        if (ex instanceof SmsException) {
            return ((SmsException) ex).getErrorCode();
        }
        if (ex instanceof TeaException) {
            TeaException tex = (TeaException) ex;
            String code = tex.getCode();
            if (!StringUtils.hasText(code) && tex.getData() != null) {
                Object dataCode = tex.getData().get("Code");
                code = dataCode == null ? null : dataCode.toString();
            }
            return mapCode(code);
        }
        String message = ex.getMessage();
        if (message != null && message.toLowerCase(Locale.ROOT).contains("throttl")) {
            return ErrorCode.PROVIDER_THROTTLED;
        }
        return ErrorCode.PROVIDER_ERROR;
    }
}

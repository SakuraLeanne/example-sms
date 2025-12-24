package com.example.sms.core;

/**
 * 短信领域异常。
 */
public class SmsException extends RuntimeException {

    private final ErrorCode errorCode;

    public SmsException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SmsException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

package com.group6.electronic_drafting_system.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Đã xảy ra lỗi. Vui lòng thử lại sau.", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_FAILED(1000, "Thông tin gửi lên không hợp lệ.", HttpStatus.BAD_REQUEST),
    BAD_REQUEST(1004, "Yêu cầu không hợp lệ.", HttpStatus.BAD_REQUEST),

    AUTH_REGISTRATION_FAILED(1100, "Đăng ký không thành công. Vui lòng kiểm tra lại thông tin.", HttpStatus.BAD_REQUEST),
    AUTH_VERIFICATION_FAILED(1101, "Mã xác minh không đúng hoặc đã hết hạn.", HttpStatus.BAD_REQUEST),
    AUTH_RESEND_OTP_FAILED(1102, "Không thể gửi lại mã xác minh. Vui lòng thử lại.", HttpStatus.BAD_REQUEST),
    AUTH_INVALID_CREDENTIALS(1103, "Email hoặc mật khẩu không đúng.", HttpStatus.UNAUTHORIZED),
    AUTH_ACCOUNT_NOT_REGISTERED(1104, "Tài khoản chưa đăng ký. Vui lòng đăng ký trước khi đăng nhập.", HttpStatus.UNAUTHORIZED),
    AUTH_ACCOUNT_NOT_VERIFIED(1105, "Tài khoản chưa xác thực OTP.", HttpStatus.UNAUTHORIZED),
    AUTH_ACCOUNT_LOCKED(1106, "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.", HttpStatus.FORBIDDEN),
    AUTH_ACCOUNT_INACTIVE(1107, "Tài khoản đã ngừng hoạt động.", HttpStatus.FORBIDDEN),
    AUTH_ACCOUNT_ALREADY_VERIFIED(1108, "Tài khoản đã được xác minh. Vui lòng đăng nhập.", HttpStatus.CONFLICT),
    AUTH_OTP_EXPIRED(1109, "Mã xác minh đã hết hạn. Vui lòng yêu cầu gửi lại mã.", HttpStatus.BAD_REQUEST),
    AUTH_OTP_TOO_MANY_ATTEMPTS(1110, "Bạn đã nhập sai quá 5 lần. Vui lòng yêu cầu gửi lại mã mới.", HttpStatus.TOO_MANY_REQUESTS),    AUTH_ACCOUNT_ALREADY_EXISTS(2000, "Email này đã được đăng ký.", HttpStatus.CONFLICT),
    AUTH_UNEXPECTED_ERROR(9999, "Đã xảy ra lỗi. Vui lòng thử lại sau.", HttpStatus.INTERNAL_SERVER_ERROR),

    ARTICLE_NOT_FOUND(3000, "Không tìm thấy bài viết.", HttpStatus.NOT_FOUND),
    ARTICLE_FORBIDDEN(3001, "Bạn không có quyền chỉnh sửa bài viết này.", HttpStatus.FORBIDDEN),
    ARTICLE_NOT_EDITABLE(3002, "Bài viết đang trong quy trình duyệt nên không thể chỉnh sửa.", HttpStatus.CONFLICT),
    ARTICLE_TITLE_REQUIRED(3003, "Tiêu đề bài viết không được để trống.", HttpStatus.BAD_REQUEST),
    ARTICLE_SAPO_REQUIRED(3004, "Cần nhập sa-pô trước khi nộp bài.", HttpStatus.BAD_REQUEST),
    ARTICLE_CONTENT_REQUIRED(3005, "Nội dung bài viết đang trống.", HttpStatus.BAD_REQUEST),
    ARTICLE_CATEGORY_REQUIRED(3006, "Chuyên mục không được để trống.", HttpStatus.BAD_REQUEST),
    ARTICLE_TOPIC_NOT_ASSIGNED(3007, "Bạn không phải người viết chính của đề tài này hoặc đề tài chưa được duyệt.", HttpStatus.FORBIDDEN),
    ARTICLE_SOURCE_NOT_FOUND(3008, "Không tìm thấy ảnh tư liệu của bài viết.", HttpStatus.NOT_FOUND),
    ARTICLE_TOPIC_REQUIRED(3009, "Vui lòng chọn đề tài cho bài viết.", HttpStatus.BAD_REQUEST),
    ARTICLE_ALREADY_EXISTS(3010, "Bạn đã có bài viết cho đề tài này.", HttpStatus.CONFLICT);

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public HttpStatus getStatusCode() { return statusCode; }

    public static ErrorCode fromName(String name) {
        if (name == null) return UNCATEGORIZED_EXCEPTION;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ex) {
            return UNCATEGORIZED_EXCEPTION;
        }
    }
}

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

    // Trưởng phòng duyệt bài
    ARTICLE_NOT_FOUND(3000, "Không tìm thấy bài viết.", HttpStatus.NOT_FOUND),
    ARTICLE_REVIEW_FORBIDDEN(3001, "Bạn không phụ trách Topic của bài này nên không được duyệt.", HttpStatus.FORBIDDEN),
    ARTICLE_NOT_SUBMITTED(3002, "Bài chưa được nộp nên chưa có phiên bản để duyệt.", HttpStatus.CONFLICT),
    ARTICLE_NOT_IN_DEPARTMENT_REVIEW(3003, "Bài không còn ở trạng thái chờ Trưởng phòng duyệt.", HttpStatus.CONFLICT),
    ARTICLE_REVIEW_TAKEN(3004, "Bài đang có lượt duyệt mở của người khác.", HttpStatus.CONFLICT),
    REVIEW_FEEDBACK_REQUIRED(3005, "Yêu cầu sửa bắt buộc phải có nhận xét.", HttpStatus.BAD_REQUEST),
    COMMENT_NOT_FOUND(3006, "Không tìm thấy góp ý.", HttpStatus.NOT_FOUND),
    COMMENT_INVALID(3007, "Góp ý không hợp lệ (thiếu nội dung hoặc sai vị trí).", HttpStatus.BAD_REQUEST);

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

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

    ARTICLE_NOT_FOUND(1300, "Không tìm thấy bài viết.", HttpStatus.NOT_FOUND),
    ARTICLE_NOT_SUBMITTED(1301, "Bài viết chưa được nộp nên chưa có phiên bản để duyệt.", HttpStatus.CONFLICT),
    REVIEW_ACCESS_DENIED(1310, "Bạn không có quyền duyệt bài ở cấp này.", HttpStatus.FORBIDDEN),
    REVIEW_NOT_IN_FINAL_STAGE(1311, "Bài viết không còn ở bước chờ Thư ký tòa soạn duyệt.", HttpStatus.CONFLICT),
    REVIEW_OWN_ARTICLE(1312, "Bạn là Lead Reporter của bài này nên không được duyệt.", HttpStatus.FORBIDDEN),
    REVIEW_HELD_BY_OTHER(1313, "Bài đang được một Thư ký tòa soạn khác duyệt.", HttpStatus.CONFLICT),
    REVIEW_DEPARTMENT_NOT_APPROVED(1314, "Phiên bản mới nhất chưa được Trưởng phòng duyệt.", HttpStatus.CONFLICT),
    REVIEW_NOT_OPENED(1315, "Lượt duyệt chưa được mở. Vui lòng mở lại bài rồi thử lại.", HttpStatus.CONFLICT),
    REVIEW_FEEDBACK_REQUIRED(1316, "Vui lòng nhập nội dung yêu cầu chỉnh sửa.", HttpStatus.BAD_REQUEST),

    ARTICLE_FORBIDDEN(1320, "Bạn không có quyền chỉnh sửa bài viết này.", HttpStatus.FORBIDDEN),
    ARTICLE_NOT_EDITABLE(1321, "Bài viết đang trong quy trình duyệt nên không thể chỉnh sửa.", HttpStatus.CONFLICT),
    ARTICLE_TITLE_REQUIRED(1322, "Tiêu đề bài viết không được để trống.", HttpStatus.BAD_REQUEST),
    ARTICLE_SAPO_REQUIRED(1323, "Cần nhập sa-pô trước khi nộp bài.", HttpStatus.BAD_REQUEST),
    ARTICLE_CONTENT_REQUIRED(1324, "Nội dung bài viết đang trống.", HttpStatus.BAD_REQUEST),
    ARTICLE_CATEGORY_REQUIRED(1325, "Chuyên mục không được để trống.", HttpStatus.BAD_REQUEST),
    ARTICLE_ASSIGNMENT_NOT_LEAD(1326, "Bạn chỉ được viết bài từ nhiệm vụ được giao cho mình với vai trò viết chính, trên đề tài đã được duyệt.", HttpStatus.FORBIDDEN),
    ARTICLE_SOURCE_NOT_FOUND(1327, "Không tìm thấy ảnh tư liệu của bài viết.", HttpStatus.NOT_FOUND),
    ARTICLE_ASSIGNMENT_REQUIRED(1328, "Bài viết phải được tạo từ một nhiệm vụ được giao. Vui lòng chọn nhiệm vụ để viết bài.", HttpStatus.BAD_REQUEST),

    TASK_NOT_FOUND(1400, "Không tìm thấy nhiệm vụ hoặc bạn không được giao nhiệm vụ này.", HttpStatus.NOT_FOUND),
    TASK_NOT_OPEN(1401, "Nhiệm vụ đã hoàn thành nên không thể thêm tư liệu.", HttpStatus.CONFLICT),
    TOPIC_SOURCE_NOT_FOUND(1402, "Không tìm thấy ảnh / video trong tư liệu của đề tài.", HttpStatus.NOT_FOUND),

    MEDIA_FILE_REQUIRED(1500, "Vui lòng chọn ảnh hoặc video để tải lên.", HttpStatus.BAD_REQUEST),
    MEDIA_TYPE_NOT_SUPPORTED(1501, "Chỉ hỗ trợ ảnh JPG, PNG, WebP, GIF và video MP4, WebM, MOV.", HttpStatus.BAD_REQUEST),
    MEDIA_TOO_LARGE(1502, "Tệp quá lớn: ảnh tối đa 10MB, video tối đa 100MB.", HttpStatus.PAYLOAD_TOO_LARGE),
    MEDIA_STORAGE_NOT_CONFIGURED(1503, "Hệ thống chưa cấu hình Cloudinary nên chưa thể tải tệp lên.", HttpStatus.SERVICE_UNAVAILABLE),
    MEDIA_UPLOAD_FAILED(1504, "Tải tệp lên Cloudinary không thành công. Vui lòng thử lại.", HttpStatus.BAD_GATEWAY);

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

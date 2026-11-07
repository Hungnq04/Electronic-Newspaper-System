package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.Account.GetAccountDTO;
import com.group6.electronic_drafting_system.entity.enums.OtpPurpose;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface AuthService {
    GetAccountDTO login(String email, String password);
    void persistSecurityContext(HttpServletRequest request);

    // Đăng ký: kiểm tra từng ô. Trả về Map<tên ô, câu lỗi>; Map rỗng nghĩa là hợp lệ
    Map<String, String> validateRegistration(String fullName, String email, String password, String confirmPassword);
    void sendRegistrationCode(String fullName, String email, String password, String confirmPassword);
    void verifyAndRegister(String email, String code);

    // Gửi lại OTP (dùng chung cho đăng ký và quên mật khẩu); chặn nếu chưa đủ 60 giây kể từ lần gửi trước
    void resendCode(String email, OtpPurpose purpose);
    // Số giây còn phải chờ mới được bấm "Gửi lại mã" (0 = bấm được ngay)
    int getResendWaitSeconds(String email, OtpPurpose purpose);

    // Quên mật khẩu: chỉ gửi mã thật cho tài khoản ACTIVE / AWAITING_APPROVAL, nhưng không cho người gọi biết
    void sendPasswordResetCode(String email);
    void verifyResetCode(String email, String code);
    // Mật khẩu mới: ít nhất 8 ký tự, có chữ và số, nhập lại phải khớp
    Map<String, String> validateNewPassword(String password, String confirmPassword);
    void resetPassword(String email, String password, String confirmPassword);

    // Đăng ký bằng Google: email chưa có thì tạo tài khoản chờ Admin duyệt (AWAITING_APPROVAL); Google đã xác minh email
    void registerWithGoogle(String email, String fullName);
    // Google đăng nhập với tài khoản còn PENDING: Google đã xác minh email nên chuyển sang AWAITING_APPROVAL (vẫn chờ Admin)
    void confirmEmailByGoogle(String email);
}

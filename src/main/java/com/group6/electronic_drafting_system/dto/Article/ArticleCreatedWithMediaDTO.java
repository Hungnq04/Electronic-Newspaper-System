package com.group6.electronic_drafting_system.dto.Article;

// Kết quả của "tải ảnh / video khi bài còn mới (chưa lưu)": hệ thống đã tự tạo bản nháp rồi gắn tệp vào.
// articleId: mã bài nháp vừa tạo (trình duyệt dùng để mở trang sửa bài); media: kết quả tải tệp như bình thường.
// Khi lỗi: success = false, message là câu báo lỗi, articleId và media là null.
public record ArticleCreatedWithMediaDTO(boolean success, String message, Integer articleId,
                                         ArticleMediaUploadResponseDTO media) {
}

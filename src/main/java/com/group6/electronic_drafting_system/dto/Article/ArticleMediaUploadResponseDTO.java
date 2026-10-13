package com.group6.electronic_drafting_system.dto.Article;

import com.group6.electronic_drafting_system.service.MediaStorageService.MediaKind;

// Kết quả tải ảnh / video lên. Ảnh được gắn làm tư liệu (articleSourceId); video chỉ chèn vào nội dung bằng url.
// featured: ảnh vừa tải được đặt làm ảnh đại diện vì bài chưa có ảnh đại diện.
public record ArticleMediaUploadResponseDTO(boolean success, String message, MediaKind kind, String url,
                                            Integer articleSourceId, String title, String caption, boolean featured) {

    public static ArticleMediaUploadResponseDTO error(String message) {
        return new ArticleMediaUploadResponseDTO(false, message, null, null, null, null, null, false);
    }
}

package com.group6.electronic_drafting_system.config;

import com.group6.electronic_drafting_system.dto.Article.ArticleMediaUploadResponseDTO;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

// Tệp vượt spring.servlet.multipart.max-file-size bị chặn trước khi vào controller: trả JSON để trình soạn thảo hiện lỗi
@RestControllerAdvice
public class UploadLimitAdvice {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ArticleMediaUploadResponseDTO> tooLarge() {
        return ResponseEntity.status(ErrorCode.MEDIA_TOO_LARGE.getStatusCode())
                .body(ArticleMediaUploadResponseDTO.error(ErrorCode.MEDIA_TOO_LARGE.getMessage()));
    }
}

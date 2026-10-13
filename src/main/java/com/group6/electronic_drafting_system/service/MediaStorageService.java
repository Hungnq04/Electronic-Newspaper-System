package com.group6.electronic_drafting_system.service;

import org.springframework.web.multipart.MultipartFile;

// Lưu ảnh / video lên kho bên ngoài (Cloudinary). DB chỉ giữ url trả về, không lưu nội dung tệp.
public interface MediaStorageService {
    StoredMedia upload(MultipartFile file, String folder);
    // Dọn tệp đã tải lên khi bước ghi DB sau đó thất bại
    void delete(StoredMedia media);

    enum MediaKind { IMAGE, VIDEO }

    record StoredMedia(MediaKind kind, String url, String publicId) {}
}

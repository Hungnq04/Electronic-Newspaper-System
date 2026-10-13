package com.group6.electronic_drafting_system.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.service.MediaStorageService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryMediaStorageService implements MediaStorageService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CloudinaryMediaStorageService.class);
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/webm", "video/quicktime");
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_VIDEO_BYTES = 100L * 1024 * 1024;
    // Video lớn tải theo từng phần để tránh một request quá dài
    private static final int CHUNK_SIZE = 20 * 1024 * 1024;

    private final Cloudinary cloudinary;
    private final String rootFolder;

    // CLOUDINARY_URL trống thì ứng dụng vẫn chạy, chỉ báo lỗi khi có người tải tệp lên
    public CloudinaryMediaStorageService(@Value("${cloudinary.url:}") String url, @Value("${cloudinary.folder:cms-core}") String rootFolder) {
        this.cloudinary = url == null || url.isBlank() ? null : new Cloudinary(url.trim());
        this.rootFolder = rootFolder;
    }

    @Override
    public StoredMedia upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) throw new ApplicationException(ErrorCode.MEDIA_FILE_REQUIRED);
        MediaKind kind = kindOf(file);
        if (file.getSize() > (kind == MediaKind.IMAGE ? MAX_IMAGE_BYTES : MAX_VIDEO_BYTES)) throw new ApplicationException(ErrorCode.MEDIA_TOO_LARGE);
        if (cloudinary == null) throw new ApplicationException(ErrorCode.MEDIA_STORAGE_NOT_CONFIGURED);

        File tmp = null;
        try {
            // Ghi ra tệp tạm thay vì nạp cả video vào bộ nhớ
            tmp = Files.createTempFile("cms-upload-", ".tmp").toFile();
            file.transferTo(tmp);
            Map<?, ?> options = ObjectUtils.asMap(
                    "resource_type", resourceType(kind),
                    "folder", rootFolder + "/" + folder,
                    "unique_filename", true,
                    "overwrite", false);
            Map<?, ?> result = kind == MediaKind.VIDEO
                    ? cloudinary.uploader().uploadLarge(tmp, options, CHUNK_SIZE)
                    : cloudinary.uploader().upload(tmp, options);
            Object url = result.get("secure_url");
            if (url == null) throw new IOException("Cloudinary không trả về secure_url: " + result.get("error"));
            return new StoredMedia(kind, url.toString(), String.valueOf(result.get("public_id")));
        } catch (IOException | RuntimeException ex) {
            if (ex instanceof ApplicationException app) throw app;
            log.error("Upload to Cloudinary failed for {}", file.getOriginalFilename(), ex);
            throw new ApplicationException(ErrorCode.MEDIA_UPLOAD_FAILED);
        } finally {
            if (tmp != null && !tmp.delete()) tmp.deleteOnExit();
        }
    }

    @Override
    public void delete(StoredMedia media) {
        if (cloudinary == null || media == null) return;
        try {
            cloudinary.uploader().destroy(media.publicId(), ObjectUtils.asMap("resource_type", resourceType(media.kind()), "invalidate", true));
        } catch (IOException | RuntimeException ex) {
            log.warn("Could not delete Cloudinary asset {}", media.publicId(), ex);
        }
    }

    private static MediaKind kindOf(MultipartFile file) {
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (IMAGE_TYPES.contains(type)) return MediaKind.IMAGE;
        if (VIDEO_TYPES.contains(type)) return MediaKind.VIDEO;
        throw new ApplicationException(ErrorCode.MEDIA_TYPE_NOT_SUPPORTED);
    }

    private static String resourceType(MediaKind kind) {
        return kind == MediaKind.IMAGE ? "image" : "video";
    }
}

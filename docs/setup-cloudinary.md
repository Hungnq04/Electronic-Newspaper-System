# Hướng dẫn cấu hình Cloudinary

Trình soạn bài (`/articles`) tải ảnh và video lên **Cloudinary**. Database chỉ lưu đường dẫn `secure_url` mà Cloudinary trả về. Cả nhóm dùng **chung một tài khoản Cloudinary**, nên ảnh do ai tải lên cũng hiển thị được trên máy của các thành viên khác.

Key Cloudinary được ghi trực tiếp trong file `src/main/resources/application.properties` ở **máy của từng người**. Bản trên git chỉ có chỗ trống, không chứa key.

Thiếu key thì ứng dụng vẫn chạy được, nhưng khi tải ảnh hoặc video lên sẽ báo lỗi *"Hệ thống chưa cấu hình Cloudinary nên chưa thể tải tệp lên."*

---

## Bước 1: Xin key

Nhắn riêng cho **Hùng** (chủ tài khoản) để xin chuỗi có dạng:

```
cloudinary://<api_key>:<api_secret>@<cloud_name>
```

> ⚠️ Đây là mật khẩu của tài khoản chung. Chỉ gửi qua tin nhắn riêng, không dán vào group công khai hay issue.

## Bước 2: Chặn git đẩy `application.properties` lên (bắt buộc)

Mở terminal ở thư mục gốc của project và chạy:

```bash
git update-index --skip-worktree src/main/resources/application.properties
```

Lệnh này báo cho git **bỏ qua mọi thay đổi** của bạn trong file này, nên key sẽ không bị commit hay push nhầm. Lệnh chỉ có tác dụng trên máy của bạn và chỉ cần chạy **một lần**.

Kiểm tra: chạy `git ls-files -v src/main/resources/application.properties`. Nếu dòng kết quả bắt đầu bằng chữ **`S`** là đã chặn thành công.

## Bước 3: Ghi key vào `application.properties`

Mở `src/main/resources/application.properties`, tìm dòng bắt đầu bằng `cloudinary.url=` rồi thay **toàn bộ** phần sau dấu `=` (ví dụ `${CLOUDINARY_URL:}`) bằng chuỗi ở Bước 1:

```properties
cloudinary.url=cloudinary://xxx:yyy@zzz
cloudinary.folder=cms-core/articles
```

Lưu ý: không thêm dấu ngoặc kép và không để khoảng trắng thừa ở hai đầu chuỗi.

## Bước 4: Chạy thử

1. Chạy (hoặc chạy lại) ứng dụng bằng `./mvnw spring-boot:run` hoặc nút Run trong IDE.
2. Đăng nhập bằng tài khoản **Phóng viên**, mở **Bài viết của tôi**, rồi mở hoặc tạo một bài.
3. Chèn một ảnh nhỏ vào nội dung bài.
4. Nếu ảnh hiện lên trong bài là đã cấu hình xong.

---

## Khi cần pull code mới có thay đổi `application.properties`

Nếu `git pull` báo lỗi liên quan tới `application.properties`, làm theo thứ tự sau:

```bash
# 1. Copy key đang có ra chỗ khác
# 2. Tạm bỏ chặn và trả file về bản gốc
git update-index --no-skip-worktree src/main/resources/application.properties
git checkout -- src/main/resources/application.properties
# 3. Pull code mới
git pull
# 4. Chặn lại, rồi ghi lại key như Bước 3
git update-index --skip-worktree src/main/resources/application.properties
```

## Giới hạn tệp

| Loại  | Định dạng            | Dung lượng tối đa |
|-------|----------------------|-------------------|
| Ảnh   | JPG, PNG, WebP, GIF  | 10 MB             |
| Video | MP4, WebM, MOV       | 100 MB            |

Mọi tệp được lưu trong thư mục `cms-core/articles` trên Cloudinary.

## Xử lý lỗi thường gặp

| Thông báo | Nguyên nhân | Cách xử lý |
|---|---|---|
| *Hệ thống chưa cấu hình Cloudinary…* | Dòng `cloudinary.url=` đang để trống | Làm lại Bước 3, rồi chạy lại ứng dụng |
| *Tải tệp lên Cloudinary không thành công…* | Key sai, có khoảng trắng hoặc dấu ngoặc thừa, hoặc mất mạng | Copy lại chuỗi chính xác, kiểm tra kết nối mạng |
| *Tệp quá lớn…* | Tệp vượt giới hạn ở bảng trên | Nén ảnh/video hoặc chọn tệp nhỏ hơn |
| *Chỉ hỗ trợ ảnh JPG, PNG…* | Sai định dạng | Đổi sang định dạng được hỗ trợ |

## Lưu ý khi dùng chung tài khoản

- **Không xóa** tệp trên Cloudinary Dashboard vì có thể đang được bài của thành viên khác sử dụng.
- Gói miễn phí có giới hạn dung lượng. Hạn chế upload video lớn khi test.
- Nếu nghi key bị lộ, báo Hùng để tạo key mới (*Dashboard → Settings → API Keys*), rồi mọi người cập nhật lại key trong file của mình.

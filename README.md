# Privacy Shield

Ứng dụng Android về quyền riêng tư, viết bằng Kotlin và Jetpack Compose Material Design 3. Dành cho máy từ Android 8.0 (API 26) trở lên. Repository chính thức: https://github.com/3hbin/PrivacyShield

Mọi danh sách ứng dụng và quyền đều đọc từ máy thật. Dữ liệu nhạy cảm nằm trên máy, mã hóa bằng EncryptedSharedPreferences. PIN và cụm khôi phục chỉ lưu dạng mã băm SHA-256 kèm muối, không lưu văn bản thuần.

## Tính năng

- Trung tâm bảo mật tổng quan và trạng thái các cơ chế bảo vệ.
- Nhật ký hoạt động cục bộ.
- Máy quét ứng dụng có tổ hợp quyền đáng chú ý.
- Khóa ứng dụng bằng PIN và sinh trắc học nếu máy hỗ trợ.
- Cảnh báo khi có phát hiện mới về camera, micrô hoặc vị trí.
- Két ghi chú mã hóa trên máy.
- Khôi phục PIN bằng cụm khôi phục và xóa dữ liệu cục bộ.
- Trợ lý mở cài đặt để người dùng tự thu hồi quyền.
- Kiểm tra cập nhật từ GitHub Releases khi người dùng bấm, có SHA-256 và xác nhận trước khi cài.
- Khóa máy nhanh qua Device Admin.
- **Chỉ báo quyền riêng tư nổi (3 chấm)**: xanh lá (camera), cam (micrô), xanh dương (vị trí). Có thể bật/tắt, đổi kích thước, độ trong suốt và kéo thả vị trí. Có màn hình xem trước.

## Giới hạn thật (quan trọng)

- Không chặn được nút nguồn, kể cả khi giữ khoảng 10 giây. Android không có API cho việc này.
- **Không biết camera, micrô hoặc vị trí đang được dùng ngay lúc này** trên Android 8/9. Ba chấm chỉ hiện khi ứng dụng đang ở trước **có quyền** tương ứng đã được cấp. Đây là sự hiện diện của quyền, không phải hoạt động cảm biến theo thời gian thực. Không giả lập phát hiện truy cập thật.
- Không tự thu hồi quyền hộ người dùng.
- Accessibility chỉ nhận tên gói ứng dụng đang ở trước, không đọc nội dung màn hình.
- Cập nhật chỉ gọi GitHub khi người dùng bấm kiểm tra. Không gửi danh sách ứng dụng đã cài.
- Chỉ báo nổi cần quyền hiển thị trên ứng dụng khác (SYSTEM_ALERT_WINDOW). Người dùng phải cấp thủ công.
- Chưa kiểm thử trên máy Huawei Y9 2019 thật. Đã biên dịch và chạy kiểm thử logic trên môi trường build.

## Đưa lên GitHub và lấy APK

1. Giải nén, tạo hoặc dùng repo https://github.com/3hbin/PrivacyShield, push lên nhánh `main`.
2. Vào **Settings → Actions** và cho phép Actions.
3. Vào **Actions → Build APK**. Chờ job xanh.
4. Tải artifact `PrivacyShield-debug` hoặc `PrivacyShield-release`.
5. Để kiểm tra cập nhật trong ứng dụng, tạo GitHub Release có file `.apk`. Có thể kèm file `.sha256`. Trong ứng dụng, nhập chủ repo và tên repo rồi bấm kiểm tra.

APK release được ký bằng debug keystore để cài được ngay. Bản cập nhật phải ký cùng chứng chỉ, nếu không Android sẽ từ chối cài đè.

## Chấm chỉ báo & lịch sử truy cập (1.2.0)

- Chấm camera/micrô chỉ hiện khi hệ thống báo cảm biến đang được dùng (CameraManager / AudioRecordingCallback).
- Lịch sử ghi lại từng phiên kèm biểu tượng ứng dụng. Tên ứng dụng là suy đoán (ứng dụng đang mở, hoặc ứng dụng có quyền đang chạy dịch vụ nền — cần quyền truy cập dữ liệu sử dụng, Android 10+). Android không cho ứng dụng thường biết chính xác ai mở cảm biến.
- Chấm vị trí chỉ dựa trên quyền của ứng dụng đang mở, không phải thời gian thực.

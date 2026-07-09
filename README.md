# Neon Cine Space - Ứng Dụng Đặt Vé Xem Phim Hiện Đại

**Neon Cine Space** là một ứng dụng di động đặt vé xem phim hiện đại, mượt mà được phát triển hoàn toàn bằng **Kotlin** và **Jetpack Compose** theo phong cách thiết kế hiện đại, trẻ trung với tông màu Neon chủ đạo. Ứng dụng tích hợp đầy đủ các chức năng cần thiết từ duyệt phim, chọn ghế, đặt vé cho đến các tính năng chăm sóc khách hàng như điểm danh nhận quà và đổi mã voucher giảm giá.

---

## 📍 Rạp Chiếu Phim Cố Định
Ứng dụng được thiết kế cố định phục vụ cho cụm rạp:
*   **Tên rạp:** Neon Cine Space - Vincom Xuân Khánh
*   **Địa chỉ:** Tầng 4, TTTM Vincom Plaza Xuân Khánh, 209 Đường 30 Tháng 4, Xuân Khánh, Ninh Kiều, Cần Thơ.
*   **Chỉ đường:** Tích hợp liên kết mở Google Maps trực tiếp để chỉ đường chính xác tới rạp Vincom Xuân Khánh.

---

## ✨ Các Tính Năng Nổi Bật

### 1. Trang Chủ & Duyệt Phim (Home & Discovery)
*   **Phim đang chiếu & Sắp chiếu:** Hiển thị danh sách phim với hình ảnh chất lượng cao (Coil), phân loại rõ ràng kèm điểm đánh giá, thời lượng và thể loại.
*   **Banner Động:** Hiển thị các phim nổi bật hàng đầu dưới dạng Slideshow/Carousel bắt mắt.
*   **Tìm kiếm & Bộ lọc:** Tìm kiếm phim theo tên, lọc nhanh theo thể loại (Hành động, Tình cảm, Viễn tưởng, Hoạt hình, Kinh dị...) giúp người dùng dễ dàng chọn phim yêu thích.
*   **Chi tiết Phim:** Xem đầy đủ thông tin mô tả cốt truyện, đạo diễn, dàn diễn viên, đánh giá, và xem Trailer phim trực tiếp.

### 2. Luồng Đặt Vé Chuyên Nghiệp (Booking Flow)
*   **Chọn Suất Chiếu:** Lựa chọn ngày xem và khung giờ chiếu phù hợp một cách trực quan.
*   **Sơ Đồ Ghế Động:** Sơ đồ phòng chiếu mô phỏng chân thực với các trạng thái ghế: *Ghế trống*, *Ghế đang chọn*, và *Ghế đã được người khác đặt trước*. Phân chia rõ ràng ghế Thường và ghế VIP.
*   **Thanh Toán Mô Phỏng:** Tính toán tổng tiền động theo loại ghế, hỗ trợ áp dụng Voucher giảm giá và thực hiện thanh toán giả lập nhanh chóng kèm mã QR thanh toán cực kỳ tiện lợi.

### 3. Cá Nhân Hóa & Tiện Ích Khách Hàng (User Profile & Rewards)
*   **Điểm Danh Hàng Ngày (Daily Check-in):** Tính năng điểm danh nhận xu thưởng hàng ngày giúp tăng tương tác và giữ chân khách hàng.
*   **Đổi Quà & Voucher:** Sử dụng xu tích lũy hoặc nhập mã quà tặng (Gift Code) để quy đổi thành các voucher giảm giá trực tiếp khi đặt vé.
*   **Lịch Sử Đặt Vé (Booking History):** Lưu trữ toàn bộ vé đã mua kèm theo trạng thái, phòng chiếu, vị trí ghế và mã QR vé để quét trực tiếp tại quầy kiểm vé của rạp.

---

## 📂 Cấu Trúc Thư Mục & Vị Trí Các File Chính

Dưới đây là sơ đồ các thư mục và file quan trọng nhất của ứng dụng để bạn dễ dàng import vào **Android Studio** và phát triển tiếp:

### 🎨 1. Các File Giao Diện (UI - Jetpack Compose)
Tất cả giao diện của ứng dụng được xây dựng bằng Jetpack Compose và nằm trong thư mục: `app/src/main/java/com/example/ui/`

*   **`screens/MainMovieApp.kt`**:
    *   *Nội dung:* Chứa giao diện chính điều hướng của ứng dụng, màn hình Trang chủ (Home), màn hình Chi tiết phim (Movie Detail), và **toàn bộ luồng chọn ghế & đặt vé xem phim**.
*   **`screens/AdditionalScreens.kt`**:
    *   *Nội dung:* Chứa các màn hình bổ trợ quan trọng:
        *   **Lịch chiếu rạp (ShowtimesScreen):** Xem chi tiết suất chiếu tại rạp Vincom Xuân Khánh, lọc theo ngày và chỉ đường qua Google Maps.
        *   **Trang cá nhân (ProfileScreen):** Giao diện thông tin tài khoản, tích hợp popup Điểm danh hàng ngày và Đổi mã Voucher quà tặng.
        *   **Lịch sử đặt vé (TicketHistoryScreen):** Danh sách tất cả các vé đã đặt thành công kèm mã QR vé xem phim.
*   **`theme/`**:
    *   *Nội dung:* Quản lý hệ màu sắc Neon bắt mắt (NeonPrimary, NeonSecondary), kiểu chữ (Typography), và cấu hình giao diện tối (Dark Theme) đồng bộ.

### ⚙️ 2. File Chức Năng & Xử Lý Logic (Business Logic)
*   **`MovieViewModel.kt`** (`app/src/main/java/com/example/ui/MovieViewModel.kt`):
    *   *Nội dung:* **Trái tim xử lý logic của toàn bộ ứng dụng**. Quản lý trạng thái UI (UI State) bằng `StateFlow`, xử lý đặt vé, tính tiền, chọn ghế, điểm danh, đổi voucher, bộ lọc tìm kiếm và lưu trữ trạng thái người dùng bằng `SharedPreferences`.

### 🛡️ 3. Cấu Hình Dự Án & Bảo Mật
*   **`build.gradle.kts` (Project & App)**: Cấu hình dependencies (Coil, Navigation Compose, v.v.).
*   **`AndroidManifest.xml`**: Khai báo quyền ứng dụng (Internet, Network State) và màn hình khởi chạy.

---

## 🛠️ Hướng Dẫn Import vào Android Studio & Chạy Dự Án

1.  **Tải/Clone mã nguồn** từ kho lưu trữ GitHub của bạn về máy tính.
2.  Mở **Android Studio** (Khuyến nghị phiên bản Koala hoặc mới hơn).
3.  Chọn **File** -> **Open** -> Chọn thư mục gốc của dự án này.
4.  Chờ Gradle đồng bộ tải các thư viện cần thiết (Sync Gradle).
5.  Kết nối thiết bị Android thật hoặc khởi động Máy ảo (Emulator).
6.  Nhấn nút **Run (Shift + F10)** để cài đặt và trải nghiệm ứng dụng!

---

## 🔒 Lưu Ý Khi Đẩy Lên GitHub
Để bảo mật và tránh đẩy các file cấu hình thừa của môi trường phát triển (AI Studio), các file sau đã được cấu hình trong `.gitignore` để **không** đẩy lên GitHub công khai:
*   `metadata.json` (File định dạng riêng của AI Studio).
*   `debug.keystore.base64` & `debug.keystore` (Khóa ký ứng dụng).
*   Thư mục `.build-outputs/` (Thư mục chứa file build APK tạm thời).

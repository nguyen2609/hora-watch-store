# Kiến trúc

## Hiện trạng M03

`Main`, `BaseEntity`, `Watch`, `AutomaticWatch`, `SmartWatch` và demo WatchOOPDemo
đã được triển khai. `Main.main(String[] args)` vẫn là điểm vào của JAR;
constructor private ngăn tạo đối tượng Main không cần thiết. Chưa chạy HTTP server,
chưa kết nối cơ sở dữ liệu. Entity Watch hiện có validation giá, thông tin và tồn kho.

## Kiến trúc dự kiến

Frontend -> Controller/Handler -> Service -> DAO -> PostgreSQL (`watch_store.app`).

| Package dưới com.mycompany.watch_store | Trách nhiệm dự kiến |
| --- | --- |
| entity | Đối tượng miền nghiệp vụ, trạng thái và quy tắc nội tại |
| dto | Dữ liệu request/response, tách khỏi entity |
| controller | HttpHandler, đọc HTTP và trả response |
| service | Điều phối nghiệp vụ, phân quyền và giao dịch |
| dao | SQL qua JDBC và PreparedStatement |
| factory | Tạo đúng loại Watch từ dữ liệu đã xác minh |
| config | Cấu hình ứng dụng và kết nối |

Package entity hiện có BaseEntity abstract với id, createdAt và validation.
Constructor mặc định dành cho entity mới (id = 0); constructor hai tham số
nhận id dương và createdAt khác null. Hai constructor protected được gọi từ lớp con.
BaseEntityTest và lớp con TestEntity đã được xóa theo yêu cầu sau M02;
chưa có ánh xạ DB. BaseEntity production được giữ nguyên.
Watch abstract kế thừa BaseEntity; AutomaticWatch và SmartWatch là lớp con final.
watchType là private final, được cố định bởi constructor của từng lớp con.
Các setter Watch dùng final để bảo toàn validation, kể cả khi gọi từ constructor.
name, brand, modelNumber và movementType yêu cầu khác null/blank;
description và imageUrl là tùy chọn ở mô hình Java, chưa khẳng định schema DB.
categoryId phải dương. Giá không âm, nguyên và tối đa 999999999999.
isInStock() chỉ phản ánh stockQuantity > 0; available là trạng thái riêng.
Tồn kho thay đổi qua setter có kiểm tra hoặc reduceStock/increaseStock;
increaseStock kiểm tra giới hạn int trước khi cộng.
WatchOOPDemo minh họa dispatch qua tham chiếu Watch và xử lý lỗi tồn kho.
Các package còn lại chỉ có package-info.java mô tả vai trò, chưa có logic.
`src/main/resources` dành cho tài nguyên ứng dụng; chưa chứa cấu hình DB hay frontend.

## Quan hệ lớp hiện tại và dự kiến

- Đã triển khai: `BaseEntity` abstract <- `Watch` abstract <- `AutomaticWatch`, `SmartWatch`.
- Các entity phù hợp khác kế thừa BaseEntity sau khi xác minh schema.
- Đã triển khai: Watch định nghĩa getWarrantyInfo(); hai lớp con override hành vi này.
- WatchService overload searchWatches(keyword) và searchWatches(keyword, maxPrice).
- `CODPaymentService` implements `PaymentService`.
- Cart chứa các CartItem; Order chứa các OrderItem và liên quan Payment.
- Watch.reduceStock() và Order.changeStatus() kiểm soát thay đổi trạng thái.

Chi tiết trường, cardinality, khóa và ánh xạ SQL phải dựa trên schema thực tế.
Không suy diễn đầy đủ schema từ tên tám bảng đã cung cấp.

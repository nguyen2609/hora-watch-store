# Online Watch Store Management System

Dự án OOP đại học sử dụng Java 17, Maven, PostgreSQL 14, JDBC, Gson,
HttpServer tích hợp Java. M01 tạo bộ khung chạy console; M02 bổ sung BaseEntity abstract.
Theo yêu cầu tiếp theo, src/test và JUnit đã được gỡ; chỉ tạo automated test
khi người dùng yêu cầu rõ ràng.

## Cài đặt và chạy

Yêu cầu JDK 17 và Maven 3.9.x. Mở thư mục dự án trong VS Code;
Extension Pack for Java hỗ trợ mở và chạy dự án Maven.

Từ PowerShell tại `C:\watch_order`, khi Maven có trên PATH:

```powershell
mvn clean verify
java -jar target/watch-store-1.0-SNAPSHOT.jar
```

Môi trường thiết lập ban đầu chỉ có JDK 24.0.1, chưa có Maven trên PATH.
Maven 3.9.11 đã được tải vào `.tools` (không commit). Trong môi trường này:

```powershell
.\.tools\apache-maven-3.9.11\bin\mvn.cmd "-Dmaven.repo.local=C:\watch_order\.m2\repository" clean verify
java -jar target/watch-store-1.0-SNAPSHOT.jar
```

Kết quả mong đợi: `Online Watch Store Management System started.`
POM dùng `maven.compiler.release=17` để giới hạn cú pháp, API Java SE và bytecode
ở Java 17 dù build bằng JDK mới hơn. Kiểm tra chạy trên JDK 17 cần runtime 17 thực tế.
JAR hiện tại chỉ cần Main; chưa đóng gói các dependency vào JAR.

## Cấu trúc thư mục

```text
pom.xml                         Cấu hình build, dependency và plugin
src/main/java/com/mycompany/watch_store/
  Main.java                     Điểm vào hiện tại
  entity/BaseEntity.java        Lớp gốc abstract, id và createdAt có validation
  config/ controller/ dao/ dto/ factory/ service/
                                Package dự kiến, chỉ có package-info.java
src/main/resources/             Tài nguyên ứng dụng, hiện trống
docs/ARCHITECTURE.md             Kiến trúc hiện tại và thiết kế dự kiến
docs/MILESTONES.md               Kế hoạch và tiêu chí từng milestone
AGENTS.md                       Quy tắc phát triển và giảng giải
target/                         Kết quả build, không commit
```

Package là namespace tổ chức lớp. Ví dụ Main có tên đầy đủ
`com.mycompany.watch_store.Main`, tương ứng đường dẫn dưới `src/main/java`.

## Maven và luồng chạy

`pom.xml` khai báo tọa độ dự án và phiên bản thư viện cố định:
PostgreSQL JDBC 42.7.14, Gson 2.14.0.
JDBC và Gson được chuẩn bị cho milestone sau; hiện chưa gọi chúng.
Nguồn phiên bản: [pgJDBC](https://jdbc.postgresql.org/),
[Gson releases](https://github.com/google/gson/releases).

Lifecycle chính: `validate -> compile -> test -> package -> verify -> install -> deploy`.
Gọi một phase sẽ chạy các phase trước nó trong cùng lifecycle.
`clean` thuộc lifecycle riêng, xóa kết quả build cũ trong target.
`compile` biên dịch; `test` chạy unit test qua Surefire; `package` tạo JAR;
`verify` hoàn thành các bước kiểm tra được cấu hình; `install` đưa artifact vào
repository cục bộ; `deploy` xuất bản artifact vào repository từ xa khi được cấu hình.
Hiện không có test tự động. Maven vẫn có phase test mặc định nhưng không có test để chạy.
Kiểm tra build bằng `mvn clean package`.

Ví dụ: `mvn clean verify` tạo Main.class và JAR. Sau đó `java -jar ...`
đọc Main-Class trong manifest, gọi `public static void main(String[] args)`,
in thông báo rồi kết thúc. Chưa có request HTTP hay truy vấn SQL.

## Cơ sở dữ liệu và phạm vi

DB hiện có: `watch_store`, schema `app`, tám bảng users, categories, watches,
carts, cart_items, orders, order_items, payments. M01 không kết nối hay thay đổi DB.
Trước mã phụ thuộc DB phải kiểm tra schema thực tế. Không lưu mật khẩu trong Git.
Chín nhóm tính năng và các quan hệ OOP sẽ được triển khai theo M02–M12;
xem tài liệu milestone. BaseEntity M02 chưa ánh xạ vào bảng DB.
M03 đã bổ sung Watch abstract, AutomaticWatch và SmartWatch; chưa có ánh xạ DB.
Chạy demo sau build:

```powershell
java -cp target/classes com.mycompany.watch_store.demo.WatchOOPDemo
```

Demo dùng tham chiếu Watch gọi bảo hành hai loại, giảm/tăng tồn kho và bắt lỗi
vượt tồn kho, tràn int. Main và entry point của JAR được giữ nguyên.
Dừng sau M03, chờ chỉ dẫn tiếp theo.

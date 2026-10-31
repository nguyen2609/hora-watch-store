# Milestones

Chỉ triển khai một milestone mỗi lần. M01–M03 đã hoàn thành; M04–M12 là kế hoạch.
Tên trường và ánh xạ DB phải được xác minh bằng schema thực tế.
Theo yêu cầu sau M02, đã gỡ src/test, dependency JUnit và cấu hình Surefire riêng.
Các mục test bên dưới là lịch sử hoặc đề xuất cần yêu cầu rõ ràng của người dùng
trước khi triển khai. Không tự tạo automated test classes từ nay.

## M01 — Project setup
- Objective: Tạo bộ khung Maven Java 17 chạy được.
- Files: pom.xml, Main.java, package-info.java, README.md, AGENTS.md,
  .gitignore, docs/ARCHITECTURE.md, docs/MILESTONES.md.
- Concepts: Maven coordinates, dependency scopes, package, main entry point, release 17.
- Tasks: Kiểm tra workspace; cấu hình JDBC/Gson/JUnit 5; tạo package và tài liệu;
  build bằng Maven và chạy JAR. Không kết nối DB.
- Testing: Chạy lệnh build và chạy ứng dụng trong README; kiểm tra bytecode bằng
  `javap -verbose target/classes/com/mycompany/watch_store/Main.class` (major version 61).
- Definition of Done: Maven clean verify thành công, JAR in đúng thông báo,
  cấu trúc và tài liệu đầy đủ; chưa có entity/DAO/service/controller/frontend logic.
- Current status: COMPLETED (2026-10-30).
- Verification: Maven 3.9.11 `clean verify` trả BUILD SUCCESS (exit 0);
  chạy JAR in `Online Watch Store Management System started.` (exit 0).
  javap xác nhận major version 61 (Java 17).
  Build/chạy thực hiện bằng JDK 24.0.1 với compiler release 17;
  chưa xác minh chạy trực tiếp bằng JDK 17. Chưa có unit test trong M01.

## M02 — BaseEntity and OOP fundamentals
- Objective: Xây dựng lớp gốc có ý nghĩa cho entity.
- Files: src/main/java/com/mycompany/watch_store/entity/BaseEntity.java;
  src/test/java/com/mycompany/watch_store/entity/BaseEntityTest.java.
- Concepts: Abstract class, inheritance, private state, constructor overloading,
  super(), encapsulation, validation, JUnit 5.
- Tasks: Triển khai id và createdAt theo yêu cầu M02 của người dùng;
  constructor mặc định cho entity mới, constructor hai tham số cho identity dương;
  từ chối id không dương và createdAt null. Chưa xác minh ánh xạ schema DB.
- Testing: Unit test constructor/validation với trường hợp hợp lệ và không hợp lệ.
- Definition of Done: Lớp gốc có trách nhiệm rõ, không khởi tạo trực tiếp; test đạt.
- Current status: COMPLETED (2026-10-30).
- Verification: Maven 3.9.11 clean test và clean package đều BUILD SUCCESS;
  mỗi lần chạy 12 test, failures 0, errors 0, skipped 0.
  Dùng JDK 24.0.1 với release 17; chưa chạy trực tiếp trên JDK 17.
  Không kết nối PostgreSQL. Git commit chỉ gồm source và cấu hình cần thiết;
  tài liệu được cập nhật trong workspace và không stage.

## M03 — Watch inheritance and polymorphism
- Objective: Mô hình hóa hai loại đồng hồ.
- Files: entity/Watch.java, AutomaticWatch.java, SmartWatch.java,
  demo/WatchOOPDemo.java. Không tạo test tự động theo yêu cầu.
- Concepts: Inheritance, overriding, dynamic dispatch, BigDecimal.
- Tasks: Triển khai thuộc tính theo yêu cầu M03, getWarrantyInfo(),
  reduceStock(), increaseStock(), isInStock() và validation.
  Chưa ánh xạ SQL; vẫn cần schema thực tế trước mã phụ thuộc database.
- Testing: Maven clean package; chạy WatchOOPDemo để gọi warranty qua tham chiếu Watch,
  giảm/tăng tồn kho hợp lệ, bắt lỗi vượt tồn và tràn int; chạy lại Main.
- Definition of Done: Build đạt; demo chạy bình thường, thể hiện đa hình và validation;
  giữ nguyên Main, BaseEntity và không tạo src/test.
- Current status: COMPLETED (2026-10-30).
- Verification: clean package BUILD SUCCESS; demo và Main chạy exit 0;
  không có test tự động. JDK 24 với release 17, chưa chạy trực tiếp trên JDK 17.

## M04 — Remaining entities and relationships
- Objective: Mô hình hóa người dùng, danh mục, giỏ, đơn và thanh toán.
- Files: entity/User.java, Category.java, Cart.java, CartItem.java,
  Order.java, OrderItem.java, Payment.java và enum/tests cần thiết.
- Concepts: Composition, association, encapsulation, controlled state changes.
- Tasks: Kiểm tra schema tám bảng; ánh xạ trường và quan hệ;
  xây dựng Order.changeStatus() cùng validation.
- Testing: Kiểm tra quan hệ và chuyển trạng thái hợp lệ/không hợp lệ.
- Definition of Done: Entity khớp schema đã kiểm tra; test quy tắc miền đạt.
- Current status: NOT_STARTED.

## M05 — PostgreSQL JDBC connection
- Objective: Kết nối an toàn DB hiện hữu.
- Files: config/DatabaseConfig.java, ConnectionFactory.java, hướng dẫn cấu hình cục bộ.
- Concepts: JDBC, configuration, try-with-resources.
- Tasks: Đọc cấu hình ngoài mã; kết nối watch_store.app; không thay đổi schema.
- Testing: Integration check kết nối và truy vấn chỉ đọc với DB được cấu hình.
- Definition of Done: Kết nối và giải phóng resource đúng; không lộ thông tin bí mật.
- Current status: NOT_STARTED.

## M06 — WatchDAO, WatchFactory and CRUD
- Objective: Truy xuất và lưu đồng hồ đúng kiểu.
- Files: dao/WatchDAO.java, factory/WatchFactory.java và tests.
- Concepts: DAO, factory, PreparedStatement, identity keys.
- Tasks: Ánh xạ schema thực tế; CRUD; tạo subtype theo watch_type.
- Testing: CRUD trên DB kiểm thử riêng, kiểm tra mapping hai subtype và input SQL.
- Definition of Done: CRUD và mapping đạt; không tác động dữ liệu thật khi test.
- Current status: NOT_STARTED.

## M07 — Authentication and WatchService
- Objective: Đăng ký, đăng nhập và tìm đồng hồ.
- Files: dao/UserDAO.java, service/AuthService.java, WatchService.java,
  thành phần hash/session và tests phù hợp.
- Concepts: Service layer, overloading, password hashing, authorization.
- Tasks: Hash mật khẩu có salt bằng thuật toán phù hợp; xác thực;
  overload searchWatches(keyword) và searchWatches(keyword, maxPrice).
- Testing: Đăng ký/trùng tài khoản; đúng/sai mật khẩu; tìm kiếm hai overload.
- Definition of Done: Không lưu mật khẩu rõ; auth và tìm kiếm qua kiểm thử.
- Current status: NOT_STARTED.

## M08 — CartDAO and CartService
- Objective: Quản lý giỏ hàng theo người dùng.
- Files: dao/CartDAO.java, service/CartService.java và tests.
- Concepts: Service orchestration, ownership, validation.
- Tasks: Xem/thêm/sửa/xóa cart item; kiểm tra số lượng và chủ sở hữu.
- Testing: Luồng cập nhật giỏ và từ chối truy cập giỏ của người khác.
- Definition of Done: Giỏ đúng người dùng; số lượng hợp lệ; test đạt.
- Current status: NOT_STARTED.

## M09 — Transactional checkout
- Objective: Tạo đơn hàng nguyên tử.
- Files: dao/OrderDAO.java, service/OrderService.java, DAO liên quan và tests.
- Concepts: Transaction, commit/rollback, concurrency, monetary precision.
- Tasks: Chung một connection trong checkout; xác minh tồn kho;
  tạo order/items/payment COD; trừ kho và xử lý giỏ trong cùng transaction.
- Testing: Checkout thành công, lỗi giữa chừng rollback, tranh chấp tồn kho.
- Definition of Done: Không tạo dữ liệu dở dang hay bán vượt tồn; test đạt.
- Current status: NOT_STARTED.

## M10 — Order management and COD payment
- Objective: Lịch sử đơn, quản trị trạng thái và xác nhận COD.
- Files: service/PaymentService.java, CODPaymentService.java,
  dao/PaymentDAO.java, cập nhật OrderService và tests.
- Concepts: Interface, implementation, authorization, state transitions.
- Tasks: Lịch sử theo người dùng; kiểm tra quyền admin; đổi trạng thái;
  xác nhận UNPAID -> PAID theo quy tắc được thống nhất.
- Testing: Chuyển trạng thái đúng/sai, truy cập trái quyền, xác nhận thanh toán lặp.
- Definition of Done: Quyền và trạng thái được kiểm soát ở server; test đạt.
- Current status: NOT_STARTED.

## M11 — REST API, DTO and frontend
- Objective: Đưa chín nhóm tính năng đến giao diện người dùng.
- Files: controller/*, dto/*, cấu hình HttpServer, resources frontend, cập nhật Main.
- Concepts: HTTP routing, DTO boundaries, Gson, session handling.
- Tasks: API đăng ký/đăng nhập/catalog/cart/checkout/history/admin;
  validation request; ánh xạ lỗi HTTP; HTML/CSS/JS gọi API.
- Testing: HTTP smoke test và luồng khách/admin; kiểm tra authorization phía server.
- Definition of Done: Chín nhóm tính năng hoạt động xuyên suốt; không lộ mật khẩu qua DTO.
- Current status: NOT_STARTED.

## M12 — Testing, UML, documentation and defense
- Objective: Kiểm chứng toàn hệ thống và chuẩn bị bảo vệ OOP.
- Files: src/test/*, docs/UML.md, docs/OOP_DEFENSE.md, README và tài liệu kiến trúc.
- Concepts: Unit/integration tests, UML, explanation of four OOP principles.
- Tasks: Hoàn thiện kiểm thử rủi ro nghiệp vụ; UML theo mã thật;
  tài liệu setup và ví dụ bảo vệ bốn nguyên lý OOP.
- Testing: Maven verify với DB kiểm thử riêng khi cần; manual end-to-end checklist.
- Definition of Done: Kiểm thử được ghi nhận trung thực; UML và hướng dẫn khớp mã;
  có thể giải thích luồng giao dịch và các quyết định thiết kế.
- Current status: NOT_STARTED.

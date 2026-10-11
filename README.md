# FilmLink Backend

Backend của đồ án **FilmLink** (FA26SE088): nền tảng kết nối cộng đồng chụp phim với các Film Lab.

Stack: Java 21 · Spring Boot 4.1.1 · PostgreSQL · Flyway · springdoc-openapi.

## 1. Yêu cầu

- **JDK 21** (`java -version` phải ra 21; nếu máy có nhiều JDK, đặt `JAVA_HOME` trỏ tới JDK 21).
- **PostgreSQL** cài trên máy (không dùng Docker), chạy ở `localhost:5432`.
- Tạo hai database:

  ```sql
  CREATE DATABASE filmlink;
  CREATE DATABASE filmlink_test;
  ```

- Cấu hình chỉ có một file `application.yml`; giá trị riêng của từng máy/môi trường đọc từ biến môi trường.
  Repo **không chứa** mật khẩu nào.
- Đặt mật khẩu PostgreSQL một lần:

  ```bat
  setx DB_PASSWORD "mật_khẩu_postgres"
  ```

  rồi **mở lại** terminal/IntelliJ để biến có hiệu lực.
- `DB_URL` (mặc định `jdbc:postgresql://localhost:5432/filmlink`) và `DB_USERNAME` (mặc định `postgres`)
  đã có giá trị mặc định cho máy local, chỉ đặt khi máy bạn khác.
- Test dùng DB `filmlink_test`, chung tài khoản `DB_USERNAME` / `DB_PASSWORD` với app.
- Khi deploy Azure: đặt `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SWAGGER_ENABLED` trong App Settings
  (`SWAGGER_ENABLED=false` để tắt Swagger UI và OpenAPI JSON).

## 2. Chạy

```bash
mvnw.cmd spring-boot:run
mvnw.cmd clean verify        # build + test (cần DB filmlink_test đang chạy)
```

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Mọi API nằm dưới `/api/v1`. Chưa đăng nhập thì trả `401`.

### Migration (Flyway)

- Thư mục: `src/main/resources/db/migration`.
- Quy ước tên: `V<yyyyMMdd>_<HHmm>__<domain>_<mo_ta>.sql`
  — ví dụ `V20261015_0930__lab_create_film_lab.sql`.
  Version theo thời điểm tạo file để 3 người làm song song trên nhiều nhánh không bị trùng.
- **Không sửa** migration đã merge vào `dev`; muốn đổi schema thì tạo migration mới.
- `ddl-auto: validate` — Hibernate chỉ kiểm tra entity khớp schema, không tự tạo bảng.
- Cột thời gian dùng `timestamptz` vì `BaseEntity` dùng `Instant`; dùng `timestamp` thì
  `ddl-auto: validate` sẽ báo lệch kiểu.

## 3. Cấu trúc thư mục

```text
src/main/java/com/fa26se088/filmlink
├── FilmLinkApplication.java
├── config/          # OpenApiConfig, JpaAuditingConfig
├── security/        # SecurityConfig (JWT làm ở task auth)
├── exception/       # ErrorCode, BusinessException, GlobalExceptionHandler (ProblemDetail)
├── utils/           # chỉ hàm static thuần
├── scheduler/       # job của actor System (Scheduler)
├── client/          # gọi hệ thống bên ngoài
│   ├── payment/       (VNPay)
│   ├── delivery/      (Delivery Partner)
│   ├── llm/           (LLM API)
│   ├── storage/       (Azure Blob Storage)
│   ├── maps/          (Maps / Geocoding)
│   └── notification/  (Email, FCM)
├── controller/<domain>/   # 15 domain
├── service/<domain>/      # 15 domain — interface; implementation trong service/<domain>/impl/
├── dto/<domain>/          # 15 domain — request/ và response/ tạo khi có file thật
├── repository/<domain>/   # 14 domain (không có dashboard)
├── entity/                # BaseEntity (id, createdAt, updatedAt)
│   └── <domain>/          # 14 domain — enums/ tạo khi có file thật
└── mapper/<domain>/       # 14 domain

src/main/resources
├── application.yml   # cấu hình duy nhất, giá trị riêng đọc từ biến môi trường
└── db/migration/
```

Mỗi package domain có `package-info.java` ghi phạm vi, UC và BR liên quan.

### 15 domain

| Domain | Nội dung | UC | BR |
|---|---|---|---|
| account | Account, role, đăng ký/đăng nhập, Lab Registration Application, Expert Upgrade Application | UC01–03, UC06–11 | BR-01–05, BR-36 |
| masterdata | Film Process, Film Format, Service Type, tham số hệ thống | UC53–54 | BR-16 |
| notification | Thông báo trong app (gửi qua client/notification) | UC04 | — |
| audit | Audit log chỉ ghi, không sửa | — | BR-32 |
| lab | Film Lab, Service Package, Price List, tìm kiếm lab | UC12–15, UC17 | BR-34, BR-37 |
| machine | Machine, Batch, Machine Incident, Promised Return Date | UC68–73 | BR-57–62 |
| delivery | Pickup, Parcel Shipping, bằng chứng bàn giao phim, Film Return | UC21, UC27 | BR-06, BR-25, BR-41 |
| archive | Scan File, Preview Scan, Digital Film Archive | UC26, UC28 | BR-08, BR-27, BR-44 |
| payment | Online Payment, Pay-at-Lab, COD, Commission Debt, Billing Period, Payout | UC30–36 | BR-09–11, BR-17–22, BR-31, BR-45–46, BR-55 |
| order | Service Order, Processing Stage | UC20, UC22–25, UC29, UC60 | BR-07, BR-23–24, BR-26, BR-38–40, BR-42 |
| marketplace | Consignment, Condition Report, Listing | UC45–48, UC61–66 | BR-13–14, BR-47, BR-49–54, BR-56 |
| community | Lab Review, Photo Feed, Forum, Expert Article | UC37–39, UC41–44 | BR-12, BR-33 |
| ai | Lab Recommendation, Advisory Chatbot, Knowledge Base, Image Analysis, Scan Quality Assessment (hướng triển khai chưa chốt) | UC18–19, UC40, UC57 | BR-28–30, BR-43 |
| moderation | Content Report, Complaint | UC49–52 | BR-15 |
| dashboard | Chỉ đọc, tổng hợp số liệu cho Admin, không có entity | UC56 | — |

## 4. Chiều phụ thuộc giữa các domain

Domain chỉ được gọi **xuống tầng dưới** (hoặc cùng tầng nếu thật cần). Khi tầng dưới cần báo cho tầng
trên (ví dụ `payment` báo `order` là đã thanh toán), dùng `ApplicationEvent` — tầng dưới publish event,
tầng trên lắng nghe — không inject service tầng trên.

| Tầng | Vai trò | Domain |
|---|---|---|
| 4 | Tổng hợp | ai, moderation, dashboard |
| 3 | Nghiệp vụ | order, marketplace, community |
| 2 | Dịch vụ cho đơn | machine, delivery, archive, payment |
| 1 | Danh mục | lab |
| 0 | Nền | account, masterdata, notification, audit |

## 5. Mười quy tắc chung

- **R1.** Chiều gọi một chiều controller → service (interface) → repository. Controller không inject repository.
- **R2.** Entity không ra khỏi service. Service nhận request DTO, trả response DTO.
- **R3.** `@Transactional` chỉ ở ServiceImpl. Method chỉ đọc dùng `readOnly = true`.
- **R4.** Repository của domain X chỉ được inject vào service của domain X. Cần dữ liệu domain khác thì gọi
  service của domain đó.
- **R5.** Request DTO có Bean Validation, controller dùng `@Valid`. Business rule kiểm trong service, ném
  `BusinessException`.
- **R6.** Controller không `try/catch`. Lỗi đi qua `GlobalExceptionHandler`, trả `ProblemDetail`.
- **R7.** URL `/api/v1/<danh-từ-số-nhiều>`. Endpoint trả danh sách nhận `Pageable`.
- **R8.** Tên class, field, enum theo glossary SRS. Method implement BR ghi mã BR trong Javadoc.
- **R9.** Gọi hệ thống bên ngoài chỉ qua `client/`.
- **R10.** `utils` chỉ chứa hàm static thuần.

Code Java theo Google Java Style (indent 2 space, không import `*`). Entity kế thừa `BaseEntity`, dùng
`@Getter`, không dùng `@Data`.

## 6. Thứ tự làm một chức năng

1. Đọc UC và BR trong SRS
2. Migration (`db/migration`)
3. Entity (`entity/<domain>`)
4. Repository (`repository/<domain>`)
5. DTO (`dto/<domain>/request`, `dto/<domain>/response`)
6. Mapper (`mapper/<domain>`)
7. Service + impl (`service/<domain>`, `service/<domain>/impl`)
8. Controller (`controller/<domain>`)
9. Test
10. Kiểm tra trên Swagger/Postman
11. Tạo PR

## 7. Quy ước git

- Nhánh: `feature/<mô-tả>`, `bugfix/<mã-defect>`; tạo từ `dev`, PR vào `dev`.
- Commit theo [Conventional Commits](https://www.conventionalcommits.org/):
  `<type>(<scope>): <mô tả> FA26-<số>`
  — ví dụ `feat(lab): add film lab search API FA26-42`.
  `type`: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`; `scope`: tên domain hoặc `setup`.

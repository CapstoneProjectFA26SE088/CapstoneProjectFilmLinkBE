# FA26SE088 — Cấu trúc Backend FilmLink

> **Phiên bản:** v0.1 · 11/10/2026
> **Áp dụng cho:** repo `FA26SE088-backend` (tên cũ `CapstoneProjectFilmLinkBE`), nhánh `dev` và mọi nhánh tách từ `dev`.
> **Đối tượng đọc:** thành viên viết backend và AI assistant (Claude Code, Copilot, Cursor…) mà thành viên dùng.
> **Nguồn đặc tả:** SRS v0.5 (`document/FA26SE088_Report3_Software-Requirement-Specification_v0.5.docx`), ERD Conceptual v0.1 (`Claude outputs/FA26SE088_ERD-Entity-Definitions_v0.1.docx`).
>
> **Ký hiệu trạng thái**
>
> - ✅ **Đã chốt** — làm theo, không cần hỏi.
> - 🟡 **[Tạm — chờ chốt]** — leader tạm chọn để không bỏ trống. Được làm theo, nhưng nếu thấy vướng thì hỏi leader trước khi lệch.
> - ⛔ **Chưa chốt** — **không được tự quyết**. Hỏi leader trước khi code phần liên quan.

---

## 0. Dành cho AI assistant — đọc trước khi viết code

Nếu bạn là AI assistant đang làm việc trong repo này, tuân thủ các quy tắc sau. Chúng ưu tiên hơn thói quen hay mẫu code phổ biến trên mạng.

1. Đọc toàn bộ file này và `README.md` của repo trước khi tạo hoặc sửa file.
2. **Chỉ đặt file vào đúng các package liệt kê ở mục 3 và 4.** Không tạo package mới ở cấp gốc. Không tạo package domain mới ngoài 15 domain ở mục 4.
3. **Tên class, field, enum phải lấy từ bảng entity ở mục 4.3 và glossary SRS.** Nếu khái niệm cần dùng không có trong bảng, dừng lại và hỏi — không tự đặt tên.
4. **Không implement phần đánh dấu ⛔.** Với phần 🟡, làm theo đúng như ghi, không tự chọn phương án khác.
5. **Không thêm dependency vào `pom.xml`** nếu task không yêu cầu rõ.
6. **Không ghi bất kỳ mật khẩu, secret, API key nào vào file** (kể cả file test, kể cả "tạm thời"). Đọc từ biến môi trường.
7. **Không sửa migration Flyway đã có trên `dev`.** Muốn đổi schema thì tạo migration mới.
8. Mỗi task chỉ làm trong phạm vi được giao. Thấy chỗ khác cần sửa thì **liệt kê ở cuối báo cáo**, không tự sửa.
9. Nếu đặc tả (SRS) và file này mâu thuẫn, **dừng lại và hỏi** — đừng chọn một bên.
10. Lưu ý phiên bản: đây là **Spring Boot 4.1 / Spring Framework 7 / Spring Security 7 / Hibernate 7 / Jackson 3**. Nhiều ví dụ trên mạng viết cho Boot 3 sẽ sai tên starter, sai package Jackson (`com.fasterxml` → `tools.jackson`) hoặc dùng API đã bỏ. Kiểm tra theo version thật trong `pom.xml`.

---

## 1. Tech stack ✅

| Thành phần | Lựa chọn                                    | Ghi chú                                                                 |
| ---------- | ------------------------------------------- | ----------------------------------------------------------------------- |
| Ngôn ngữ   | **Java 21**                                 | Máy có nhiều JDK thì `JAVA_HOME` và `PATH` phải trỏ JDK 21 (xem mục 10) |
| Framework  | **Spring Boot 4.1.1**                       | Starter web là `spring-boot-starter-webmvc` (không phải `-web`)         |
| Database   | **PostgreSQL**                              | Mỗi người tự cài trên máy, không dùng Docker. pgvector chưa dùng        |
| Migration  | **Flyway**                                  | `spring-boot-starter-flyway` + `flyway-database-postgresql`             |
| ORM        | Spring Data JPA (Hibernate 7)               | `ddl-auto: validate` — Hibernate **không** tự tạo bảng                  |
| Bảo mật    | Spring Security 7                           | Cơ chế JWT ⛔ chưa chốt (mục 8.4)                                       |
| API docs   | springdoc-openapi 3.1.1                     | Swagger UI tại `/swagger-ui.html`                                       |
| Tiện ích   | Lombok                                      | Không dùng `@Data` trên entity                                          |
| Test       | JUnit 5, Mockito, Spring Boot Test, MockMvc | DB test là `filmlink_test`                                              |
| Build      | Maven Wrapper (`mvnw.cmd`)                  | Không cần cài Maven riêng                                               |

**Kiến trúc:** layered truyền thống (controller → service → repository), **bên trong mỗi layer chia sub-package theo domain**. ✅

---

## 2. Nguyên lý tổ chức trong 30 giây

- **Layer** trả lời "file này làm _việc kỹ thuật_ gì": nhận HTTP, xử lý nghiệp vụ, truy cập DB.
- **Domain** trả lời "file này nói về _chủ đề nghiệp vụ_ gì": đơn dịch vụ, thanh toán, máy…
- Mỗi file có cả hai: `service/order/impl/ServiceOrderServiceImpl.java` là layer **service**, domain **order**.
- Domain ≠ actor ≠ luồng nghiệp vụ. **Không** tạo thư mục kiểu `photographer/`, `filmlab/`, `checkout-flow/`.
- Mỗi khái niệm nghiệp vụ nằm ở **đúng một** domain. Mỗi BR thuộc về **đúng một** domain.

---

## 3. Cây thư mục ✅

```text
src/main/java/com/fa26se088/filmlink/
├── FilmLinkApplication.java
├── config/                  # @Configuration: OpenApiConfig, JpaAuditingConfig, ...
├── security/                # SecurityConfig và mọi thứ về xác thực / phân quyền hạ tầng
├── exception/               # ErrorCode, BusinessException, GlobalExceptionHandler (ProblemDetail)
├── utils/                   # CHỈ hàm static thuần — không Spring bean, không business rule
├── scheduler/               # Job của actor System (Scheduler)
├── client/                  # Gọi hệ thống bên ngoài (mục 6)
│   ├── payment/             #   VNPay
│   ├── delivery/            #   Delivery Partner (Pickup nội thành, Parcel Shipping liên tỉnh)
│   ├── llm/                 #   LLM API
│   ├── storage/             #   Azure Blob Storage
│   ├── maps/                #   Maps / Geocoding
│   └── notification/        #   Email, Firebase Cloud Messaging
├── controller/<domain>/     # 15 domain
├── service/<domain>/        # 15 domain — interface
│   └── impl/                #   implementation (tạo khi có file)
├── dto/<domain>/            # 15 domain
│   ├── request/             #   (tạo khi có file)
│   └── response/            #   (tạo khi có file)
├── repository/<domain>/     # 14 domain (không có dashboard)
├── entity/
│   ├── BaseEntity.java      # id, createdAt, updatedAt
│   └── <domain>/            # 14 domain (không có dashboard)
│       └── enums/           #   (tạo khi có file)
└── mapper/<domain>/         # 14 domain (không có dashboard)

src/main/resources/
├── application.yml          # file cấu hình DUY NHẤT của app
└── db/migration/            # Flyway

src/test/java/com/fa26se088/filmlink/   # cùng cấu trúc package với main
src/test/resources/application-test.yml # chỉ ghi đè URL sang DB filmlink_test
```

Mỗi package domain có sẵn `package-info.java` ghi phạm vi, UC và BR. **Đọc file đó trước khi thêm class vào package.**

### 3.1 Vai trò từng thư mục

| Thư mục                 | Chứa gì                                                                                              | Không được chứa                                                  |
| ----------------------- | ---------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------- |
| `controller/<domain>`   | `@RestController`: nhận request DTO, gọi service, trả response DTO                                   | Repository, entity, `@Transactional`, `try/catch`, business rule |
| `service/<domain>`      | Interface service                                                                                    | —                                                                |
| `service/<domain>/impl` | Implementation: business rule, `@Transactional`, gọi repository cùng domain, gọi service domain khác | Repository của domain khác, chi tiết API bên thứ ba              |
| `repository/<domain>`   | `JpaRepository` của entity cùng domain                                                               | Logic nghiệp vụ                                                  |
| `entity/<domain>`       | JPA entity kế thừa `BaseEntity`                                                                      | `@Data`, logic gọi service                                       |
| `entity/<domain>/enums` | Enum của domain (trạng thái, loại…)                                                                  | —                                                                |
| `dto/<domain>/request`  | Request DTO (Java `record`) có Bean Validation                                                       | Entity                                                           |
| `dto/<domain>/response` | Response DTO (Java `record`)                                                                         | Mật khẩu, token, dữ liệu thừa (NFR-PRI-04)                       |
| `mapper/<domain>`       | Chuyển entity ↔ DTO                                                                                  | Gọi repository, business rule                                    |
| `client/<hệ-thống>`     | Interface + implementation thật + mock cho hệ thống bên ngoài                                        | Business rule                                                    |
| `scheduler`             | `@Scheduled` job, gọi service                                                                        | Business rule (để trong service)                                 |
| `utils`                 | Hàm static thuần (format tiền VND, sinh mã…)                                                         | Spring bean, business rule                                       |
| `security`              | Cấu hình Security, xử lý token, entry point                                                          | Logic đăng ký / đăng nhập (thuộc `service/account`)              |
| `exception`             | `ErrorCode`, `BusinessException`, `GlobalExceptionHandler`                                           | Exception riêng cho từng loại lỗi (dùng `ErrorCode`)             |

---

## 4. Domain

### 4.1 Mười lăm domain ✅

| Domain         | Phạm vi                                                                                       | UC                        | BR chính                                   |
| -------------- | --------------------------------------------------------------------------------------------- | ------------------------- | ------------------------------------------ |
| `account`      | Tài khoản, role, đăng ký / đăng nhập, hồ sơ đăng ký lab, hồ sơ nâng cấp Expert                | UC01–03, UC06–11          | BR-01–05, BR-36                            |
| `masterdata`   | Film Process, Film Format, Service Type, tham số hệ thống                                     | UC53–54                   | BR-16                                      |
| `notification` | Thông báo trong app (gửi qua `client/notification`)                                           | UC04                      | CR-12                                      |
| `audit`        | Audit log chỉ ghi, không sửa                                                                  | —                         | BR-32, CR-16, CR-20                        |
| `lab`          | Film Lab, Service Package (bảng giá), tìm kiếm lab                                            | UC12–15, UC17             | BR-34, BR-37                               |
| `machine`      | Machine, Development Batch, Scan Job, Machine Incident, Promised Return Date                  | UC68–73                   | BR-57–62                                   |
| `delivery`     | Pickup, Parcel Shipping, Film Handover (bằng chứng bàn giao), Film Return                     | UC21, UC27                | BR-06, BR-25, BR-41                        |
| `archive`      | Scan File, Preview Scan, Digital Film Archive, Rescan Request                                 | UC26, UC28                | BR-08, BR-27, BR-44                        |
| `payment`      | Payment, Commission, Commission Debt, Billing Period, Sanction, Payout                        | UC30–36                   | BR-09–11, BR-17–22, BR-31, BR-45–46, BR-55 |
| `order`        | Service Order, Film Roll, Processing Stage, Order Change Request                              | UC20, UC22–25, UC29, UC60 | BR-07, BR-23–24, BR-26, BR-38–40, BR-42    |
| `marketplace`  | Consignment (ký gửi), Condition Report, Listing, mua bán, trả hàng                            | UC45–48, UC61–66          | BR-13–14, BR-47, BR-49–54, BR-56           |
| `community`    | Lab Review, Photo Feed, Forum, Expert Article                                                 | UC37–39, UC41–44          | BR-12, BR-33                               |
| `ai`           | Lab Recommendation, Advisory Chatbot, Knowledge Base, Image Analysis, Scan Quality Assessment | UC18–19, UC40, UC57       | BR-28–30, BR-43                            |
| `moderation`   | Content Report, Complaint                                                                     | UC49–52                   | BR-15                                      |
| `dashboard`    | Chỉ đọc, tổng hợp số liệu cho Admin. **Không có entity / repository / mapper**                | UC56                      | —                                          |

**Không có backend** (Tier = Simulated UI): UC16 và UC55 (Subscription Plan), UC58–59 (Workshop / Photowalk), UC67 (Seller Rating). Khi được nâng tier mới thêm code.

**UC05 (xem nội dung công khai)** không thuộc riêng domain nào: mỗi domain `lab`, `community`, `marketplace` tự mở endpoint GET công khai của mình.

### 4.2 Các ca lưng chừng đã quyết ✅

| Khái niệm                        | Thuộc domain                        | Lý do                                                   |
| -------------------------------- | ----------------------------------- | ------------------------------------------------------- |
| Machine                          | `machine` (không phải `lab`)        | Có vòng đời, thuật toán xếp lịch, mục SRS 3.11 riêng    |
| Commission Debt, Payout          | `payment` (không tách `commission`) | Cùng là tiền, cùng người làm. Phình quá thì tách sau    |
| Film Handover (bằng chứng BR-25) | `delivery`                          | Khái niệm bàn giao, kể cả Self Drop-off                 |
| Scan Quality Assessment          | `ai` (không phải `archive`)         | Mọi tính năng AI ở một chỗ                              |
| Master Data, System Parameter    | `masterdata` (không phải `admin`)   | Nhiều domain đọc, nên ở tầng nền                        |
| Lab Review                       | `community` (không phải `lab`)      | Nội dung do người dùng viết, bị kiểm duyệt như bài đăng |
| Lab Registration Application     | `account`                           | Duyệt xong thì tạo tài khoản Film Lab Owner (BR-03)     |

### 4.3 Entity → domain → tên class 🟡

Danh sách lấy từ ERD Conceptual v0.1 (53 khái niệm). **Cột, khoá và quan hệ chi tiết sẽ do ERD Logical (Report 4) quyết định** — chưa có thì hỏi leader trước khi tạo bảng.

| Domain       | Entity (ERD)                                                       | Tên class Java                                                 | Ghi chú                                                           |
| ------------ | ------------------------------------------------------------------ | -------------------------------------------------------------- | ----------------------------------------------------------------- |
| account      | User Account                                                       | `UserAccount`                                                  | Tránh trùng tên với `User` của Spring Security                    |
| account      | Photographer, Photography Expert, Film Lab Owner, Moderator, Admin | enum `Role`                                                    | Là **role**, không phải bảng riêng. Cách mô hình hoá ⛔ (mục 8.4) |
| account      | Expert Upgrade Application                                         | `ExpertUpgradeApplication`                                     | Tier Should                                                       |
| account      | Lab Registration Application                                       | `LabRegistrationApplication`                                   |                                                                   |
| masterdata   | Service Type / Film Process / Film Format                          | `ServiceType`, `FilmProcess`, `FilmFormat`                     | Chỉ Admin tạo (BR-16)                                             |
| masterdata   | System Parameter                                                   | `SystemParameter`                                              |                                                                   |
| notification | Notification                                                       | `Notification`                                                 |                                                                   |
| audit        | Audit Log                                                          | `AuditLog`                                                     | Chỉ insert                                                        |
| lab          | Film Lab                                                           | `FilmLab`                                                      | Actor là _Film Lab Owner_, thực thể là _Film Lab_                 |
| lab          | Service Package                                                    | `ServicePackage`                                               | Bảng giá = danh sách Service Package                              |
| lab          | Subscription Plan                                                  | `SubscriptionPlan`                                             | Simulated UI — chưa code                                          |
| machine      | Machine                                                            | `Machine`                                                      | Loại `PROCESSOR` / `SCANNER`                                      |
| machine      | Machine Incident                                                   | `MachineIncident`                                              |                                                                   |
| machine      | Development Batch                                                  | `DevelopmentBatch`                                             | Glossary SRS gọi là _Batch_                                       |
| machine      | Scan Job                                                           | `ScanJob`                                                      |                                                                   |
| order        | Service Order                                                      | `ServiceOrder`                                                 | **Không** đặt tên `Order`                                         |
| order        | Film Roll                                                          | `FilmRoll`                                                     | Đơn vị xếp lịch                                                   |
| order        | Processing Stage Record                                            | `ProcessingStageRecord`                                        | Các stage là enum `ProcessingStage` (BR-07)                       |
| order        | Order Change Request                                               | `OrderChangeRequest`                                           | BR-40                                                             |
| delivery     | Film Handover                                                      | `FilmHandover`                                                 |                                                                   |
| archive      | Scan File                                                          | `ScanFile`                                                     | Preview Scan + Full-resolution Link (BR-08)                       |
| archive      | Rescan Request                                                     | `RescanRequest`                                                | BR-27                                                             |
| payment      | Payment                                                            | `Payment`                                                      | Xem cảnh báo mục 12                                               |
| payment      | Commission                                                         | `Commission`                                                   |                                                                   |
| payment      | Commission Debt                                                    | `CommissionDebt`                                               | Kỳ công nợ: Billing Period                                        |
| payment      | Payout                                                             | `Payout`                                                       | Chuyển khoản thủ công (BR-31)                                     |
| payment      | Sanction                                                           | `Sanction`                                                     | BR-21                                                             |
| marketplace  | Consignment Booking / Item                                         | `ConsignmentBooking`, `ConsignmentItem`                        |                                                                   |
| marketplace  | Condition Report                                                   | `ConditionReport`                                              | BR-51                                                             |
| marketplace  | Price Offer                                                        | `PriceOffer`                                                   | Tối đa 3 vòng (BR-52)                                             |
| marketplace  | Consignment Listing / Sale / Handover                              | `ConsignmentListing`, `ConsignmentSale`, `ConsignmentHandover` |                                                                   |
| marketplace  | Return Request                                                     | `ReturnRequest`                                                | BR-56                                                             |
| marketplace  | Seller Rating                                                      | `SellerRating`                                                 | Simulated UI — chưa code                                          |
| community    | Lab Review                                                         | `LabReview`                                                    | Một đơn một review (BR-33)                                        |
| community    | Photo Feed Post / Comment                                          | `PhotoFeedPost`, `Comment`                                     |                                                                   |
| community    | Forum Thread / Answer                                              | `ForumThread`, `ForumAnswer`                                   |                                                                   |
| community    | Expert Article                                                     | `ExpertArticle`                                                |                                                                   |
| community    | Workshop Event                                                     | `WorkshopEvent`                                                | Simulated UI — chưa code                                          |
| ai           | Knowledge Base Entry                                               | `KnowledgeBaseEntry`                                           | Hướng làm AI ⛔ (mục 12)                                          |
| ai           | AI Interaction                                                     | `AiInteraction`                                                |                                                                   |
| moderation   | Content Report                                                     | `ContentReport`                                                |                                                                   |
| moderation   | Complaint                                                          | `Complaint`                                                    | Khiếu nại liên quan tiền, Admin xử lý                             |

Quan hệ nhiều–nhiều (like bài Photo Feed, đăng ký Workshop) là bảng nối, không phải entity riêng.

---

## 5. Chiều phụ thuộc giữa các domain ✅

```text
Tầng 4  Tổng hợp         ai · moderation · dashboard
Tầng 3  Nghiệp vụ        order · marketplace · community
Tầng 2  Dịch vụ cho đơn  machine · delivery · archive · payment
Tầng 1  Danh mục         lab
Tầng 0  Nền              account · masterdata · notification · audit
```

- Service của domain **chỉ được gọi service của domain ở tầng thấp hơn** (hoặc cùng tầng nếu thật cần và không tạo vòng).
- **Chiều ngược lên tầng trên phải dùng `ApplicationEvent`**: tầng dưới publish event, tầng trên lắng nghe. Không inject service tầng trên — Spring sẽ báo lỗi vòng phụ thuộc khi khởi động.
- Ba chỗ đã biết cần event:

| Event                                | Phát → nghe                          | Lý do                                             |
| ------------------------------------ | ------------------------------------ | ------------------------------------------------- |
| Thanh toán thành công                | `payment` → `order`, `marketplace`   | `order` đã gọi `payment` để tạo giao dịch         |
| Lịch máy thay đổi                    | `machine` → `order`                  | UC71: máy hỏng phải cập nhật Promised Return Date |
| Đã upload scan / bài viết được duyệt | `archive` → `ai`, `community` → `ai` | `ai` ở tầng trên                                  |

---

## 6. Client — gọi hệ thống bên ngoài ✅

`client/` là "phiên dịch viên" giữa nghiệp vụ của mình và API bên thứ ba. Service chỉ biết **interface**; URL, chữ ký HMAC, mã lỗi của bên thứ ba chỉ nằm trong `client/`.

| Hệ thống (SRS 4.1)                                                                          | Package               | Domain dùng           |
| ------------------------------------------------------------------------------------------- | --------------------- | --------------------- |
| VNPay sandbox                                                                               | `client/payment`      | payment, marketplace  |
| Delivery Partner — Pickup nội thành, Parcel Shipping liên tỉnh (**chưa chọn nhà cung cấp**) | `client/delivery`     | delivery              |
| LLM API                                                                                     | `client/llm`          | ai                    |
| Azure Blob Storage                                                                          | `client/storage`      | archive, community    |
| Maps / Geocoding                                                                            | `client/maps`         | lab, ai, delivery     |
| Email Service, Firebase Cloud Messaging                                                     | `client/notification` | account, notification |

Quy tắc:

- Interface đặt tên `<Hệ thống>Client` (ví dụ `DeliveryPartnerClient`). Mỗi interface có **bản mock** + bản thật; chọn bản qua property, ví dụ `filmlink.delivery.provider: mock`. 🟡
- Client trả về kiểu dữ liệu của mình (`PaymentResult`), **không** trả nguyên response của bên thứ ba lên service.
- Webhook (VNPay IPN, Delivery Partner) đi vào `controller/<domain>` → `service/<domain>` → service nhờ client xác thực chữ ký. Xử lý webhook phải **idempotent** (SRS 4.1).

---

## 7. Mười quy tắc chung ✅

| #   | Quy tắc                                                                                                             | Đúng                                                                             | Sai                                                                   |
| --- | ------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| R1  | Chiều gọi một chiều controller → service (interface) → repository                                                   | Controller inject `ServiceOrderService`                                          | Controller inject `ServiceOrderRepository`                            |
| R2  | Entity không ra khỏi service. Service nhận request DTO, trả response DTO                                            | Trả `ServiceOrderResponse`                                                       | Trả `ServiceOrder` (lộ dữ liệu, lỗi lazy loading)                     |
| R3  | `@Transactional` chỉ ở `ServiceImpl`. Method chỉ đọc dùng `readOnly = true`                                         | `@Transactional(readOnly = true)` trên method tìm kiếm                           | `@Transactional` trên controller                                      |
| R4  | Repository của domain X chỉ được inject vào service của domain X                                                    | `LabRecommendationServiceImpl` gọi `CommissionDebtService.hasOverdueDebt(labId)` | `LabRecommendationServiceImpl` inject `CommissionDebtRepository`      |
| R5  | Request DTO có Bean Validation, controller dùng `@Valid`. Business rule kiểm trong service, ném `BusinessException` | `@NotBlank @Email String email`                                                  | Kiểm `if (email == null)` trong controller                            |
| R6  | Controller không `try/catch`. Lỗi đi qua `GlobalExceptionHandler`, trả `ProblemDetail`                              | `throw new BusinessException(ErrorCode.X, "...")`                                | `catch (RuntimeException e) { return badRequest(); }`                 |
| R7  | URL `/api/v1/<danh-từ-số-nhiều-kebab-case>`. Endpoint trả danh sách nhận `Pageable`                                 | `GET /api/v1/service-orders?page=0&size=20`                                      | `GET /getAllOrders` trả cả bảng                                       |
| R8  | Tên theo glossary SRS / bảng 4.3. Method implement BR ghi mã BR trong Javadoc                                       | `/** BR-24: tự đóng đơn sau 7 ngày */`                                           | Class tên `Order`, `Shop`, `Bill`                                     |
| R9  | Gọi hệ thống bên ngoài chỉ qua `client/`                                                                            | Service gọi `PaymentGatewayClient`                                               | Service tự gọi `RestClient` tới VNPay                                 |
| R10 | `utils` chỉ chứa hàm static thuần                                                                                   | `MoneyUtils.formatVnd(150000)`                                                   | `OrderUtils.calculatePromisedDate(...)` (đây là BR-60, thuộc service) |

Các quy tắc này **chưa** được kiểm tự động (ArchUnit chưa thêm). Người review PR kiểm bằng tay.

---

## 8. Quy ước chi tiết

### 8.1 Đặt tên 🟡

| Loại         | Quy ước                                                                                        | Ví dụ                                          |
| ------------ | ---------------------------------------------------------------------------------------------- | ---------------------------------------------- |
| Entity       | Thuật ngữ glossary, PascalCase, số ít                                                          | `ServiceOrder`                                 |
| Bảng         | snake_case, số nhiều                                                                           | `service_orders`                               |
| Cột          | snake_case. Khoá chính `id` (bigint identity). Khoá ngoại `<entity>_id`                        | `film_lab_id`                                  |
| Enum         | Trong `entity/<domain>/enums`, giá trị UPPER_SNAKE_CASE, lưu DB dạng chuỗi (`EnumType.STRING`) | `ServiceOrderStatus.AWAITING_LAB_CONFIRMATION` |
| Service      | `<Entity>Service` + `<Entity>ServiceImpl`                                                      | `ServiceOrderServiceImpl`                      |
| Controller   | `<Entity>Controller`                                                                           | `ServiceOrderController`                       |
| Request DTO  | `<Hành động><Entity>Request` (record)                                                          | `CreateServiceOrderRequest`                    |
| Response DTO | `<Entity>Response`; danh sách rút gọn dùng `<Entity>SummaryResponse`                           | `ServiceOrderSummaryResponse`                  |
| Mapper       | `<Entity>Mapper`                                                                               | `ServiceOrderMapper`                           |
| Event        | `<Entity><Quá khứ>Event`, đặt trong `service/<domain-phát>/event/`                             | `PaymentSucceededEvent`                        |
| Job          | `<Hành động>Job` trong `scheduler/`                                                            | `ServiceOrderAutoCloseJob`                     |
| ErrorCode    | `<DOMAIN>_<LÝ_DO>`                                                                             | `ORDER_STAGE_SKIPPED`                          |
| Test         | `<Class>Test`                                                                                  | `ServiceOrderServiceImplTest`                  |

Mapper viết tay hay dùng MapStruct: ⛔ chưa chốt — trước mắt viết tay, không thêm MapStruct.

### 8.2 Persistence ✅

- Mọi thay đổi schema qua Flyway trong `src/main/resources/db/migration/`.
- Tên migration: `V<yyyyMMdd>_<HHmm>__<domain>_<mo_ta>.sql`, ví dụ `V20261015_0930__lab_create_film_labs.sql`. Version theo thời điểm để 3 người làm song song không trùng.
- **Không sửa migration đã merge vào `dev`.**
- `ddl-auto: validate`: entity và bảng phải khớp, nếu không app không khởi động.
- Entity kế thừa `BaseEntity` (`id`, `createdAt`, `updatedAt`). Dùng `@Getter`, `@Setter` khi cần; **không dùng `@Data`**.
- Cột thời gian dùng `timestamptz` (vì `BaseEntity` dùng `Instant`). Lưu UTC, hiển thị GMT+7 ở FE (CR-04).
- Tiền: VND, không có phần lẻ (CR-05) → Java `Long`, cột `bigint`. 🟡
- Quan hệ `@ManyToOne` / `@OneToMany` mặc định `fetch = LAZY`. `open-in-view: false`, nên dữ liệu lazy phải được nạp trong service.
- Bản ghi nghiệp vụ (Service Order, Lab Review, Listing, Photo Feed Post) **xoá mềm** bằng đổi trạng thái (CR-15).

### 8.3 API ✅

- Base path `/api/v1`.
- Status code: `200` đọc / cập nhật · `201` tạo mới · `204` không có body · `400` sai định dạng · `401` chưa đăng nhập · `403` không đủ quyền · `404` không tìm thấy · `409` xung đột (trùng, sai trạng thái) · `422` vi phạm business rule.
- Lỗi luôn trả `ProblemDetail`, có property `errorCode`. **Không** trả stack trace, tên bảng hay mã lỗi kỹ thuật (CR-06).
- Mã thông báo hiển thị (MSG01–MSG37, Appendix 5.3 SRS): backend trả kèm mã, FE giữ câu tiếng Việt (CR-13). ⛔ Cần thống nhất với FE trước khi làm.
- Danh sách: phân trang mặc định 20 bản ghi (CR-01), có sắp xếp mặc định (CR-02).
- Phân quyền kiểm ở **tầng service** theo ma trận SRS 3.1.3 (CR-14). Ẩn nút ở FE không được tính là phân quyền.

### 8.4 Bảo mật

| Hạng mục                                                               | Trạng thái                          |
| ---------------------------------------------------------------------- | ----------------------------------- |
| Mặc định chặn mọi request trừ Swagger; chưa đăng nhập trả 401          | ✅ đã có trong `SecurityConfig`     |
| Không `permitAll` cho cả `/api/v1/**`                                  | ✅                                  |
| Mật khẩu hash bằng BCrypt (NFR-SEC-03)                                 | ✅                                  |
| Secret chỉ đọc từ biến môi trường (NFR-SEC-05)                         | ✅                                  |
| Không log mật khẩu, token, OTP ở môi trường thật (NFR-PRI-04)          | ✅                                  |
| 401 trả body rỗng, chưa phải `ProblemDetail`                           | 🟡 Sửa trong task auth              |
| Lỗi 403 từ phân quyền theo method đang bị handler chung biến thành 500 | 🟡 Thêm handler 403 trong task auth |

**Yêu cầu auth theo SRS** (người làm task auth phải đáp ứng):

| UC                         | Yêu cầu                                                                                                                                                                                                                                                                                                                                                                                       |
| -------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| UC06 Đăng ký Photographer  | Email duy nhất; mật khẩu ≥ 8 ký tự gồm chữ và số; họ tên 2–100 ký tự; bắt buộc đồng ý điều khoản. Tài khoản ở trạng thái chưa xác thực → gửi OTP email (6 số, hiệu lực 5 phút, sai tối đa 5 lần, gửi lại sau 60 giây, tối đa 3 lần / 15 phút) → kích hoạt và tự đăng nhập. Email trùng trả lỗi chung chung. Chưa xác thực quá 24 giờ thì xoá. Chỉ có thể tạo role Photographer (BR-01, BR-03) |
| UC01 Đăng nhập / đăng xuất | Access token 15 phút, refresh token 7 ngày, có thể thu hồi (NFR-SEC-01). Đăng xuất thu hồi refresh token. Sai thông tin trả MSG09 chung. Sai 5 lần trong 15 phút → khoá 15 phút (MSG10, NFR-SEC-07). Tài khoản bị khoá → báo lý do, không cấp token. Chưa xác thực email → chuyển sang bước OTP. Token mang role; Admin có toàn quyền Moderator (BR-05)                                       |

**⛔ Các quyết định auth chưa chốt — hỏi leader trước khi code:**

1. Mô hình role. Đề xuất: enum 5 role, Expert là đổi role trên cùng tài khoản, dùng `RoleHierarchy` (`PHOTOGRAPHY_EXPERT` ⊃ `PHOTOGRAPHER`, `ADMIN` ⊃ `MODERATOR`).
2. Cách làm JWT. Đề xuất: Spring Security OAuth2 Resource Server ký HS256 bằng `JWT_SECRET`, thay vì jjwt + filter tự viết.
3. Gửi email OTP. Đề xuất: `client/notification` có bản mock (in OTP ra log, chỉ khi `filmlink.mail.provider=mock`) cho tới khi có SMTP.
4. Định dạng mã thông báo trả cho FE (mục 8.3).
5. Trạng thái tài khoản. Đề xuất: `PENDING_VERIFICATION`, `ACTIVE`, `LOCKED` (+ lý do); khoá tạm do sai mật khẩu dùng trường `loginLockedUntil`.
6. Tạo tài khoản Admin đầu tiên. Đề xuất: tự tạo khi khởi động nếu có biến `ADMIN_EMAIL`, `ADMIN_PASSWORD`; không ghi vào migration.

---

## 9. Cấu hình và biến môi trường ✅

Chỉ có **một** file cấu hình app: `src/main/resources/application.yml`. Giá trị khác nhau giữa các máy / môi trường đọc từ biến môi trường.

| Biến              | Bắt buộc | Mặc định                                    | Dùng cho                                     |
| ----------------- | -------- | ------------------------------------------- | -------------------------------------------- |
| `DB_PASSWORD`     | **Có**   | — (thiếu thì app không khởi động)           | Mật khẩu PostgreSQL, dùng cho cả app và test |
| `DB_URL`          | Không    | `jdbc:postgresql://localhost:5432/filmlink` | Đổi khi DB không ở máy local                 |
| `DB_USERNAME`     | Không    | `postgres`                                  |                                              |
| `SWAGGER_ENABLED` | Không    | `true`                                      | Đặt `false` để tắt Swagger trên Azure        |

Biến cho các task sau (JWT, Admin, SMTP, VNPay, LLM, Azure Blob…) thêm vào bảng này khi được chốt. **Không bao giờ đặt giá trị mặc định cho secret.**

`src/test/resources/application-test.yml` chỉ đổi URL sang DB `filmlink_test`, dùng chung `DB_USERNAME` / `DB_PASSWORD`.

---

## 10. Cài đặt máy ✅

1. **JDK 21.** Kiểm tra `java -version` ra 21. Nếu máy còn JDK 17: trong _System variables → Path_, thêm `C:\Program Files\Java\jdk-21\bin` và đẩy lên **trên cùng** (System Path được đọc trước User Path). Trong IntelliJ: _File → Project Structure → SDK_ chọn JDK 21.
2. **PostgreSQL** chạy ở `localhost:5432`. Tạo hai DB: `CREATE DATABASE filmlink;` và `CREATE DATABASE filmlink_test;`.
3. **Đặt mật khẩu DB** trong PowerShell thường (không chạy Administrator):
   ```powershell
   setx DB_PASSWORD "mat-khau-postgres-cua-ban"
   reg query HKCU\Environment /v DB_PASSWORD   # kiểm tra đã lưu
   ```
   Sau đó **đóng hẳn** terminal, IntelliJ, Claude Code rồi mở lại.
4. Chạy:
   ```powershell
   .\mvnw.cmd clean verify        # build + test
   .\mvnw.cmd spring-boot:run     # Swagger: http://localhost:8080/swagger-ui.html
   ```
5. IntelliJ: mở **thư mục repo** (File → Open → thư mục chứa `pom.xml`), không mở thư mục cha.

---

## 11. Quy trình làm một chức năng ✅

**Thứ tự viết code:**

1. Đọc UC và BR liên quan trong SRS; ghi mã UC / BR vào task Jira `FA26-xx`.
2. Migration Flyway.
3. Entity (+ enum).
4. Repository.
5. DTO request / response.
6. Mapper.
7. Service interface → impl (BR ở đây, ghi mã BR trong Javadoc).
8. Controller.
9. Test: mỗi BR ít nhất một unit test cho service; MockMvc cho controller.
10. Kiểm trên Swagger / Postman.
11. Mở PR vào `dev`.

**Checklist trước khi mở PR:**

- [ ] `mvnw.cmd clean verify` pass trên máy mình.
- [ ] File nằm đúng package (mục 3, 4); tên theo bảng 4.3.
- [ ] Không vi phạm R1–R10.
- [ ] Không có secret trong diff.
- [ ] Migration mới đặt tên đúng quy ước, không sửa migration cũ.
- [ ] Endpoint mới có trong Swagger, có phân quyền đúng ma trận SRS 3.1.3.
- [ ] Mỗi BR implement có mã BR trong Javadoc và có test.

**Git:**

- Nhánh: `main` (deploy) · `dev` (tích hợp) · `feature/<mô-tả>` · `bugfix/<mã-defect>`. Tạo nhánh từ `dev`, PR vào `dev`, không commit thẳng vào `dev` / `main`.
- Commit theo Conventional Commits: `<type>(<scope>): <mô tả> FA26-<số>`. `type`: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`. `scope`: tên domain hoặc `setup`. Ví dụ: `feat(lab): add film lab search API FA26-42`.
- Code style: Google Java Style (indent 2 space, không import `*`).

---

## 12. Trạng thái hiện tại và việc còn treo

| Hạng mục                                                                                           | Trạng thái                                                                                                                                                                                |
| -------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Khung dự án (cấu trúc, cấu hình, `SecurityConfig`, `GlobalExceptionHandler`, `BaseEntity`, README) | Đã dựng trên nhánh `feature/project-skeleton`, **chưa commit**, **chưa chạy test / khởi động app** (chờ PostgreSQL và `DB_PASSWORD` trên máy leader)                                      |
| Code cũ (register / login của bản trước)                                                           | Đã bỏ hoàn toàn, không dùng lại                                                                                                                                                           |
| Auth (UC01, UC06)                                                                                  | Giao cho thành viên; các quyết định ⛔ ở mục 8.4 phải hỏi leader                                                                                                                          |
| Hướng làm AI (Java + pgvector hay service Python riêng)                                            | ⛔ Chưa chốt. Domain `ai` chỉ có `package-info.java`. pgvector trên Windows phải tự build — thử trước khi chọn hướng Java                                                                 |
| Thanh toán                                                                                         | ⛔ SRS v0.5 (BR-17–19, MSG17) vẫn ghi **đặt cọc 30%**, nhưng quyết định 06/10/2026 đổi sang **trả 100%**; SRS chưa sửa. **Không implement đặt cọc** — hỏi leader trước khi code `payment` |
| Nhà cung cấp Delivery Partner                                                                      | Chưa chọn — dùng mock trong `client/delivery`                                                                                                                                             |
| ArchUnit (kiểm R1, R3, R4 tự động)                                                                 | Chưa thêm                                                                                                                                                                                 |
| Checkstyle trong CI, GitHub Actions                                                                | Chưa thêm                                                                                                                                                                                 |
| Mapper viết tay hay MapStruct                                                                      | ⛔ Chưa chốt — viết tay                                                                                                                                                                   |
| Swagger trên Azure                                                                                 | Mặc định bật (`SWAGGER_ENABLED=true`); quyết định tắt / mở khi deploy                                                                                                                     |

---

## 13. Lịch sử thay đổi

| Phiên bản | Ngày       | Thay đổi                                                                                |
| --------- | ---------- | --------------------------------------------------------------------------------------- |
| v0.1      | 11/10/2026 | Bản đầu: kiến trúc layered + 15 domain, quy tắc R1–R10, quy ước, trạng thái khung dự án |

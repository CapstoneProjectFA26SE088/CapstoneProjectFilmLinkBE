-- Khung dự án FilmLink: khởi tạo lịch sử migration.
-- Chưa có bảng nghiệp vụ. Mỗi domain thêm bảng bằng migration riêng theo quy ước
-- V<yyyyMMdd>_<HHmm>__<domain>_<mo_ta>.sql (xem README).

CREATE TABLE account (
                         account_id     BIGSERIAL    PRIMARY KEY,
                         password       VARCHAR(255) NOT NULL,
                         email          VARCHAR(100),
                         phone_number   VARCHAR(20),
                         full_name      VARCHAR(100) NOT NULL,
                         gender         VARCHAR(20)  NOT NULL,
                         account_status VARCHAR(50)  NOT NULL
);

CREATE UNIQUE INDEX uq_account_email ON account (email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX uq_account_phone_number ON account (phone_number) WHERE phone_number IS NOT NULL;

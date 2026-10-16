/**
 * Job định kỳ. Job của actor System (Scheduler).
 *
 * <p>Quy ước (chi tiết ở README, mục "Quy ước dùng chung"):
 *
 * <ul>
 *   <li>Tên class {@code <Domain><Hành động>Job}, ví dụ {@code OrderAutoCloseJob}.
 *   <li>Job mỏng: chỉ gọi service; business rule và mã BR nằm trong service.
 *   <li>Cron đọc từ {@code filmlink.jobs.<tên-job>.cron} trong {@code application.yml}, ví dụ
 *       {@code @Scheduled(cron = "${filmlink.jobs.order-auto-close.cron}", zone =
 *       "Asia/Ho_Chi_Minh")}.
 *   <li>Luôn ghi {@code zone = "Asia/Ho_Chi_Minh"}: JVM chạy UTC, thiếu zone thì job "1 giờ sáng"
 *       chạy lúc 8 giờ sáng giờ Việt Nam. Không viết {@code Asia/Saigon}.
 *   <li>Idempotent: chạy lại (hoặc chạy trễ) không làm sai dữ liệu.
 *   <li>Log số bản ghi đã xử lý.
 *   <li>Thao tác nhạy cảm ghi audit log với actor {@code null} (System).
 *   <li>Bật/tắt toàn bộ bằng {@code filmlink.scheduling.enabled} ({@code SchedulingConfig}); test
 *       đặt {@code false}.
 * </ul>
 */
package com.fa26se088.filmlink.scheduler;

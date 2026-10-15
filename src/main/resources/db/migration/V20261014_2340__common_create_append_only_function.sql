-- Nguồn: docs/db/C2-machine-order-delivery-archive.sql (Cluster 2, Version v0.1 · 14/10/2026)
-- Bảng: (không có bảng) hàm trg_append_only()

-- Shared helper: evidence and history tables are append-only (BR-25, NFR-REL-03, NFR-DAT-03).
CREATE FUNCTION trg_append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION '% is append-only: % is not allowed', TG_TABLE_NAME, TG_OP;
END;
$$ LANGUAGE plpgsql;

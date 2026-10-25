-- Nguồn: docs/db/C4-marketplace-consignment.sql (Cluster 4, Version v0.1 · 19/10/2026)
-- Bảng: (không có bảng mới) ALTER shipments: thêm consignment_sale_id, fk_shipments_sale, ck_shipments_owner, ck_shipments_direction (thêm TO_BUYER), ck_shipments_direction_owner, ix_shipments_sale

-- Shipments (cluster 2) now also carry consignment deliveries (UC62, UC63).
ALTER TABLE shipments ALTER COLUMN service_order_id DROP NOT NULL;
ALTER TABLE shipments ADD COLUMN consignment_sale_id bigint;
ALTER TABLE shipments ADD CONSTRAINT fk_shipments_sale
    FOREIGN KEY (consignment_sale_id) REFERENCES consignment_sales (id);
ALTER TABLE shipments ADD CONSTRAINT ck_shipments_owner
    CHECK (num_nonnulls(service_order_id, consignment_sale_id) = 1);
ALTER TABLE shipments DROP CONSTRAINT ck_shipments_direction;
ALTER TABLE shipments ADD CONSTRAINT ck_shipments_direction
    CHECK (direction IN ('TO_LAB', 'RETURN_TO_PHOTOGRAPHER', 'TO_BUYER'));
ALTER TABLE shipments ADD CONSTRAINT ck_shipments_direction_owner
    CHECK ((direction = 'TO_BUYER') = (consignment_sale_id IS NOT NULL));
-- ix_shipments_order (cluster 2) becomes partial in effect: NULL keys are skipped by lookups.
CREATE INDEX ix_shipments_sale ON shipments (consignment_sale_id) WHERE consignment_sale_id IS NOT NULL;

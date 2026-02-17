-- CORE STOCK TABLE
CREATE TABLE stock (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    item_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    opening_qty INTEGER NOT NULL DEFAULT 0,
    in_qty INTEGER NOT NULL DEFAULT 0,
    out_qty INTEGER NOT NULL DEFAULT 0,
    closing_qty INTEGER NOT NULL DEFAULT 0,
    average_cost DECIMAL(18, 2) DEFAULT 0,
    stock_value DECIMAL(18, 2) DEFAULT 0,
    CONSTRAINT uk_stock_item_warehouse_tenant UNIQUE (item_id, warehouse_id, tenant_id)
);

CREATE INDEX idx_stock_uuid ON stock(uuid);
CREATE INDEX idx_stock_item_id ON stock(item_id);
CREATE INDEX idx_stock_tenant_id ON stock(tenant_id);
CREATE INDEX idx_stock_warehouse_id ON stock(warehouse_id);
CREATE INDEX idx_stock_closing_qty ON stock(closing_qty);

CREATE TABLE stock_batch (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    item_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    batch_number VARCHAR(100) NOT NULL,
    grn_id BIGINT,
    buy_price DECIMAL(18, 2),
    initial_qty INTEGER NOT NULL,
    remaining_qty INTEGER NOT NULL,
    expiry_date BIGINT,
    CONSTRAINT fk_stock_batch_grn FOREIGN KEY (grn_id) REFERENCES goods_receipt(id) ON DELETE SET NULL,
    CONSTRAINT uk_stock_batch_number UNIQUE (batch_number, warehouse_id, item_id, tenant_id)
);

CREATE INDEX idx_stock_batch_uuid ON stock_batch(uuid);
CREATE INDEX idx_stock_batch_item_id ON stock_batch(item_id);
CREATE INDEX idx_stock_batch_warehouse_id ON stock_batch(warehouse_id);
CREATE INDEX idx_stock_batch_batch_number ON stock_batch(batch_number);
CREATE INDEX idx_stock_batch_expiry_date ON stock_batch(expiry_date);

CREATE TABLE stock_ledger (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    item_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL, -- Values: IN, OUT
    quantity INTEGER NOT NULL,
    reference_type VARCHAR(50), -- Values: GRN, SALE, TRANSFER, etc.
    reference_id BIGINT,
    before_qty INTEGER NOT NULL,
    after_qty INTEGER NOT NULL,
    unit_price DECIMAL(18, 2),
    total_value DECIMAL(18, 2)
);

CREATE INDEX idx_stock_ledger_uuid ON stock_ledger(uuid);
CREATE INDEX idx_stock_ledger_item_id ON stock_ledger(item_id);
CREATE INDEX idx_stock_ledger_warehouse_id ON stock_ledger(warehouse_id);
CREATE INDEX idx_stock_ledger_transaction_type ON stock_ledger(transaction_type);
CREATE INDEX idx_stock_ledger_reference_type ON stock_ledger(reference_type);

CREATE TABLE stock_adjustment (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    tenant_id BIGINT NOT NULL,
    adjustment_number VARCHAR(50) UNIQUE NOT NULL,
    warehouse_id BIGINT NOT NULL,
    adjustment_date TIMESTAMP NOT NULL,
    adjustment_type VARCHAR(50) NOT NULL, -- Values: DAMAGE, EXPIRED, etc.
    adjustment_status VARCHAR(50) NOT NULL, -- Values: DRAFT, COMPLETED, CANCELLED
    reference VARCHAR(100),
    remarks TEXT
);

CREATE TABLE stock_adjustment_item (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    adjustment_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    system_qty INTEGER NOT NULL,
    counted_qty INTEGER NOT NULL,
    difference_qty INTEGER NOT NULL,
    reason_type VARCHAR(50) NOT NULL,
    CONSTRAINT fk_adjustment_item_adjustment FOREIGN KEY (adjustment_id) REFERENCES stock_adjustment(id) ON DELETE CASCADE
);

CREATE INDEX idx_stock_adjustment_uuid ON stock_adjustment(uuid);
CREATE INDEX idx_stock_adjustment_status ON stock_adjustment(adjustment_status); -- Index on new column name

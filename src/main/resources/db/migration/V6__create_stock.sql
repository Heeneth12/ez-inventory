-- ==================================================================================
-- STOCK MANAGEMENT SYSTEM - COMPLETE MIGRATION
-- ==================================================================================
-- This migration creates all tables for inventory tracking, batch management,
-- stock adjustments, and ledger entries

-- Create movement_type enum
CREATE TYPE movement_type AS ENUM (
    'IN',
    'OUT'
);

-- Create reference_type enum
CREATE TYPE reference_type AS ENUM (
    'GRN',
    'SALE',
    'PURCHASE_RETURN',
    'SALES_RETURN',
    'TRANSFER',
    'PRODUCTION',
    'ADJUSTMENT',
    'OPENING_STOCK',
    'RESERVATION',
    'CONSUMPTION',
    'WRITE_OFF'
);

-- Create adjustment_type enum
CREATE TYPE adjustment_type AS ENUM (
    'DAMAGE',
    'EXPIRED',
    'AUDIT_CORRECTION',
    'FOUND_EXTRA',
    'LOST'
);

-- Create adjustment_status enum
CREATE TYPE adjustment_status AS ENUM (
    'DRAFT',
    'COMPLETED',
    'CANCELLED',
    'REJECTED',
    'PENDING_APPROVAL'
);

-- ==================================================================================
-- CORE STOCK TABLE
-- ==================================================================================

-- Create stock table (Summary/Aggregate view per item-warehouse)
CREATE TABLE stock (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Stock specific fields
    item_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    opening_qty INTEGER NOT NULL DEFAULT 0,
    in_qty INTEGER NOT NULL DEFAULT 0,
    out_qty INTEGER NOT NULL DEFAULT 0,
    closing_qty INTEGER NOT NULL DEFAULT 0,
    average_cost DECIMAL(18, 2) DEFAULT 0,
    stock_value DECIMAL(18, 2) DEFAULT 0,

    -- Unique constraint: one stock record per item-warehouse-tenant
    CONSTRAINT uk_stock_item_warehouse_tenant UNIQUE (item_id, warehouse_id, tenant_id)
);

-- Create indexes for stock
CREATE INDEX idx_stock_uuid ON stock(uuid);
CREATE INDEX idx_stock_item_id ON stock(item_id);
CREATE INDEX idx_stock_tenant_id ON stock(tenant_id);
CREATE INDEX idx_stock_warehouse_id ON stock(warehouse_id);
CREATE INDEX idx_stock_closing_qty ON stock(closing_qty);
CREATE INDEX idx_stock_is_deleted ON stock(is_deleted);
CREATE INDEX idx_stock_created_at ON stock(created_at);
CREATE INDEX idx_stock_updated_at ON stock(updated_at);

-- Composite indexes for common queries
CREATE INDEX idx_stock_tenant_warehouse ON stock(tenant_id, warehouse_id)
    WHERE is_deleted = false;
CREATE INDEX idx_stock_item_warehouse ON stock(item_id, warehouse_id)
    WHERE is_deleted = false;
CREATE INDEX idx_stock_low_stock ON stock(tenant_id, closing_qty)
    WHERE is_deleted = false AND closing_qty <= 10;

-- Add comments
COMMENT ON TABLE stock IS 'Aggregate stock summary per item-warehouse combination';
COMMENT ON COLUMN stock.opening_qty IS 'Opening quantity at start of period';
COMMENT ON COLUMN stock.in_qty IS 'Total quantity received (cumulative)';
COMMENT ON COLUMN stock.out_qty IS 'Total quantity sold/consumed (cumulative)';
COMMENT ON COLUMN stock.closing_qty IS 'Current available quantity (opening + in - out)';
COMMENT ON COLUMN stock.average_cost IS 'Weighted average cost per unit';
COMMENT ON COLUMN stock.stock_value IS 'Total value of stock (closing_qty * average_cost)';

-- ==================================================================================
-- BATCH TRACKING TABLE
-- ==================================================================================

-- Create stock_batch table (FIFO/LIFO tracking)
CREATE TABLE stock_batch (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Batch specific fields
    item_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    batch_number VARCHAR(100) NOT NULL,
    grn_id BIGINT,
    buy_price DECIMAL(18, 2),
    initial_qty INTEGER NOT NULL,
    remaining_qty INTEGER NOT NULL,
    expiry_date BIGINT,

    -- Foreign key constraint
    CONSTRAINT fk_stock_batch_grn FOREIGN KEY (grn_id)
        REFERENCES goods_receipt(id) ON DELETE SET NULL,

    -- Unique constraint: one batch per batch_number-warehouse-item
    CONSTRAINT uk_stock_batch_number UNIQUE (batch_number, warehouse_id, item_id, tenant_id)
);

-- Create indexes for stock_batch
CREATE INDEX idx_stock_batch_uuid ON stock_batch(uuid);
CREATE INDEX idx_stock_batch_item_id ON stock_batch(item_id);
CREATE INDEX idx_stock_batch_warehouse_id ON stock_batch(warehouse_id);
CREATE INDEX idx_stock_batch_tenant_id ON stock_batch(tenant_id);
CREATE INDEX idx_stock_batch_batch_number ON stock_batch(batch_number);
CREATE INDEX idx_stock_batch_grn_id ON stock_batch(grn_id);
CREATE INDEX idx_stock_batch_expiry_date ON stock_batch(expiry_date);
CREATE INDEX idx_stock_batch_remaining_qty ON stock_batch(remaining_qty);
CREATE INDEX idx_stock_batch_is_deleted ON stock_batch(is_deleted);
CREATE INDEX idx_stock_batch_created_at ON stock_batch(created_at);
CREATE INDEX idx_stock_batch_updated_at ON stock_batch(updated_at);

-- Composite indexes for common queries
CREATE INDEX idx_stock_batch_item_warehouse ON stock_batch(item_id, warehouse_id)
    WHERE is_deleted = false;
CREATE INDEX idx_stock_batch_available ON stock_batch(item_id, warehouse_id, remaining_qty)
    WHERE is_deleted = false AND remaining_qty > 0;
CREATE INDEX idx_stock_batch_expiring ON stock_batch(tenant_id, expiry_date)
    WHERE is_deleted = false AND remaining_qty > 0;
CREATE INDEX idx_stock_batch_fifo ON stock_batch(item_id, warehouse_id, created_at)
    WHERE is_deleted = false AND remaining_qty > 0;

-- Add comments
COMMENT ON TABLE stock_batch IS 'Batch-level stock tracking for FIFO/LIFO and expiry management';
COMMENT ON COLUMN stock_batch.batch_number IS 'Unique batch identifier (e.g., GRN-101-BATCH, BATCH-JAN-001)';
COMMENT ON COLUMN stock_batch.grn_id IS 'Link to goods receipt where this batch was received';
COMMENT ON COLUMN stock_batch.buy_price IS 'Specific purchase cost for this batch';
COMMENT ON COLUMN stock_batch.initial_qty IS 'Original quantity received in this batch';
COMMENT ON COLUMN stock_batch.remaining_qty IS 'Current available quantity in this batch';
COMMENT ON COLUMN stock_batch.expiry_date IS 'Unix timestamp of expiry date';

-- ==================================================================================
-- STOCK LEDGER TABLE (Transaction Log)
-- ==================================================================================

-- Create stock_ledger table (Detailed transaction history)
CREATE TABLE stock_ledger (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Ledger specific fields
    item_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    transaction_type movement_type NOT NULL,
    quantity INTEGER NOT NULL,
    reference_type reference_type,
    reference_id BIGINT,
    before_qty INTEGER NOT NULL,
    after_qty INTEGER NOT NULL,
    unit_price DECIMAL(18, 2),
    total_value DECIMAL(18, 2)
);

-- Create indexes for stock_ledger
CREATE INDEX idx_stock_ledger_uuid ON stock_ledger(uuid);
CREATE INDEX idx_stock_ledger_item_id ON stock_ledger(item_id);
CREATE INDEX idx_stock_ledger_tenant_id ON stock_ledger(tenant_id);
CREATE INDEX idx_stock_ledger_warehouse_id ON stock_ledger(warehouse_id);
CREATE INDEX idx_stock_ledger_transaction_type ON stock_ledger(transaction_type);
CREATE INDEX idx_stock_ledger_reference_type ON stock_ledger(reference_type);
CREATE INDEX idx_stock_ledger_reference_id ON stock_ledger(reference_id);
CREATE INDEX idx_stock_ledger_is_deleted ON stock_ledger(is_deleted);
CREATE INDEX idx_stock_ledger_created_at ON stock_ledger(created_at);
CREATE INDEX idx_stock_ledger_updated_at ON stock_ledger(updated_at);

-- Composite indexes for common queries
CREATE INDEX idx_stock_ledger_item_warehouse ON stock_ledger(item_id, warehouse_id)
    WHERE is_deleted = false;
CREATE INDEX idx_stock_ledger_tenant_date ON stock_ledger(tenant_id, created_at)
    WHERE is_deleted = false;
CREATE INDEX idx_stock_ledger_reference ON stock_ledger(reference_type, reference_id)
    WHERE is_deleted = false;
CREATE INDEX idx_stock_ledger_item_date ON stock_ledger(item_id, created_at DESC)
    WHERE is_deleted = false;

-- Add comments
COMMENT ON TABLE stock_ledger IS 'Complete audit trail of all stock movements';
COMMENT ON COLUMN stock_ledger.transaction_type IS 'Direction of movement: IN or OUT';
COMMENT ON COLUMN stock_ledger.quantity IS 'Quantity moved (always positive)';
COMMENT ON COLUMN stock_ledger.reference_type IS 'Type of transaction triggering this movement';
COMMENT ON COLUMN stock_ledger.reference_id IS 'ID of the source transaction (GRN ID, Invoice ID, etc.)';
COMMENT ON COLUMN stock_ledger.before_qty IS 'Stock quantity before this transaction';
COMMENT ON COLUMN stock_ledger.after_qty IS 'Stock quantity after this transaction';
COMMENT ON COLUMN stock_ledger.unit_price IS 'Price per unit (purchase or selling price)';
COMMENT ON COLUMN stock_ledger.total_value IS 'Total value of transaction (quantity * unit_price)';

-- ==================================================================================
-- STOCK ADJUSTMENT TABLES
-- ==================================================================================

-- Create stock_adjustment table (Physical count adjustments)
CREATE TABLE stock_adjustment (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Adjustment specific fields
    tenant_id BIGINT NOT NULL,
    adjustment_number VARCHAR(50) UNIQUE NOT NULL,
    warehouse_id BIGINT NOT NULL,
    adjustment_date TIMESTAMP NOT NULL,
    reason_type adjustment_type NOT NULL,
    status adjustment_status NOT NULL,
    reference VARCHAR(100),
    remarks TEXT
);

-- Create stock_adjustment_item table
CREATE TABLE stock_adjustment_item (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Adjustment item specific fields
    adjustment_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    system_qty INTEGER NOT NULL,
    counted_qty INTEGER NOT NULL,
    difference_qty INTEGER NOT NULL,
    reason_type adjustment_type NOT NULL,

    -- Foreign key constraint
    CONSTRAINT fk_adjustment_item_adjustment FOREIGN KEY (adjustment_id)
        REFERENCES stock_adjustment(id) ON DELETE CASCADE
);

-- Create indexes for stock_adjustment
CREATE INDEX idx_stock_adjustment_uuid ON stock_adjustment(uuid);
CREATE INDEX idx_stock_adjustment_tenant_id ON stock_adjustment(tenant_id);
CREATE INDEX idx_stock_adjustment_warehouse_id ON stock_adjustment(warehouse_id);
CREATE INDEX idx_stock_adjustment_number ON stock_adjustment(adjustment_number);
CREATE INDEX idx_stock_adjustment_date ON stock_adjustment(adjustment_date);
CREATE INDEX idx_stock_adjustment_reason_type ON stock_adjustment(reason_type);
CREATE INDEX idx_stock_adjustment_status ON stock_adjustment(status);
CREATE INDEX idx_stock_adjustment_is_deleted ON stock_adjustment(is_deleted);
CREATE INDEX idx_stock_adjustment_created_at ON stock_adjustment(created_at);
CREATE INDEX idx_stock_adjustment_updated_at ON stock_adjustment(updated_at);

-- Composite indexes for stock_adjustment
CREATE INDEX idx_stock_adjustment_tenant_status ON stock_adjustment(tenant_id, status)
    WHERE is_deleted = false;
CREATE INDEX idx_stock_adjustment_warehouse_date ON stock_adjustment(warehouse_id, adjustment_date)
    WHERE is_deleted = false;

-- Create indexes for stock_adjustment_item
CREATE INDEX idx_adjustment_item_uuid ON stock_adjustment_item(uuid);
CREATE INDEX idx_adjustment_item_adjustment_id ON stock_adjustment_item(adjustment_id);
CREATE INDEX idx_adjustment_item_item_id ON stock_adjustment_item(item_id);
CREATE INDEX idx_adjustment_item_reason_type ON stock_adjustment_item(reason_type);
CREATE INDEX idx_adjustment_item_is_deleted ON stock_adjustment_item(is_deleted);
CREATE INDEX idx_adjustment_item_created_at ON stock_adjustment_item(created_at);
CREATE INDEX idx_adjustment_item_updated_at ON stock_adjustment_item(updated_at);

-- Composite indexes for stock_adjustment_item
CREATE INDEX idx_adjustment_item_differences ON stock_adjustment_item(adjustment_id, difference_qty)
    WHERE is_deleted = false AND difference_qty != 0;

-- Add comments
COMMENT ON TABLE stock_adjustment IS 'Physical stock count adjustments';
COMMENT ON TABLE stock_adjustment_item IS 'Line items for stock adjustments';

COMMENT ON COLUMN stock_adjustment.adjustment_number IS 'Unique adjustment number (e.g., ADJ-2025-0001)';
COMMENT ON COLUMN stock_adjustment.reason_type IS 'Primary reason for adjustment';
COMMENT ON COLUMN stock_adjustment.status IS 'Processing status of adjustment';
COMMENT ON COLUMN stock_adjustment.reference IS 'External reference number';

COMMENT ON COLUMN stock_adjustment_item.system_qty IS 'Expected quantity in system';
COMMENT ON COLUMN stock_adjustment_item.counted_qty IS 'Actual physical count';
COMMENT ON COLUMN stock_adjustment_item.difference_qty IS 'Variance (counted - system)';
COMMENT ON COLUMN stock_adjustment_item.reason_type IS 'Specific reason for this item adjustment';

-- ==================================================================================
-- SUMMARY
-- ==================================================================================

COMMENT ON TYPE movement_type IS 'Stock movement direction';
COMMENT ON TYPE reference_type IS 'Transaction type that triggered stock movement';
COMMENT ON TYPE adjustment_type IS 'Reason for stock adjustment';
COMMENT ON TYPE adjustment_status IS 'Stock adjustment processing status';
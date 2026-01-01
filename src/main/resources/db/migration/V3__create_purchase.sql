-- Create po_status enum
CREATE TYPE po_status AS ENUM (
    'DRAFT',
    'ISSUED',
    'PARTIALLY_RECEIVED',
    'COMPLETED',
    'CANCELLED'
);

-- Create purchase_order table
CREATE TABLE purchase_order (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Purchase order specific fields
    tenant_id BIGINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    supplier_name VARCHAR(255) NOT NULL,
    warehouse_id BIGINT NOT NULL,
    order_number VARCHAR(100) UNIQUE NOT NULL,
    order_date BIGINT,
    expected_delivery_date BIGINT,
    status po_status,
    total_amount DECIMAL(18, 2),
    notes TEXT
);

-- Create purchase_order_item table
CREATE TABLE purchase_order_item (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Purchase order item specific fields
    purchase_order_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    ordered_qty INTEGER NOT NULL,
    received_qty INTEGER NOT NULL DEFAULT 0,
    unit_price DECIMAL(18, 2),
    line_total DECIMAL(18, 2),

    -- Foreign key constraint
    CONSTRAINT fk_po_item_purchase_order FOREIGN KEY (purchase_order_id)
        REFERENCES purchase_order(id) ON DELETE CASCADE
);

-- Create indexes for purchase_order
CREATE INDEX idx_purchase_order_uuid ON purchase_order(uuid);
CREATE INDEX idx_purchase_order_tenant_id ON purchase_order(tenant_id);
CREATE INDEX idx_purchase_order_supplier_id ON purchase_order(supplier_id);
CREATE INDEX idx_purchase_order_warehouse_id ON purchase_order(warehouse_id);
CREATE INDEX idx_purchase_order_order_number ON purchase_order(order_number);
CREATE INDEX idx_purchase_order_status ON purchase_order(status);
CREATE INDEX idx_purchase_order_order_date ON purchase_order(order_date);
CREATE INDEX idx_purchase_order_expected_delivery_date ON purchase_order(expected_delivery_date);
CREATE INDEX idx_purchase_order_is_deleted ON purchase_order(is_deleted);
CREATE INDEX idx_purchase_order_created_at ON purchase_order(created_at);
CREATE INDEX idx_purchase_order_updated_at ON purchase_order(updated_at);

-- Composite indexes for common queries
CREATE INDEX idx_purchase_order_tenant_status ON purchase_order(tenant_id, status)
    WHERE is_deleted = false;
CREATE INDEX idx_purchase_order_tenant_supplier ON purchase_order(tenant_id, supplier_id)
    WHERE is_deleted = false;
CREATE INDEX idx_purchase_order_warehouse_status ON purchase_order(warehouse_id, status)
    WHERE is_deleted = false;

-- Create indexes for purchase_order_item
CREATE INDEX idx_po_item_uuid ON purchase_order_item(uuid);
CREATE INDEX idx_po_item_purchase_order_id ON purchase_order_item(purchase_order_id);
CREATE INDEX idx_po_item_item_id ON purchase_order_item(item_id);
CREATE INDEX idx_po_item_is_deleted ON purchase_order_item(is_deleted);
CREATE INDEX idx_po_item_created_at ON purchase_order_item(created_at);
CREATE INDEX idx_po_item_updated_at ON purchase_order_item(updated_at);

-- Add table and column comments
COMMENT ON TABLE purchase_order IS 'Purchase orders for inventory procurement';
COMMENT ON TABLE purchase_order_item IS 'Line items for purchase orders';

COMMENT ON COLUMN purchase_order.order_number IS 'Unique PO number (e.g., PO-2023-001)';
COMMENT ON COLUMN purchase_order.status IS 'PO lifecycle status';
COMMENT ON COLUMN purchase_order.order_date IS 'Unix timestamp of order creation';
COMMENT ON COLUMN purchase_order.expected_delivery_date IS 'Unix timestamp of expected delivery';

COMMENT ON COLUMN purchase_order_item.ordered_qty IS 'Quantity ordered from supplier';
COMMENT ON COLUMN purchase_order_item.received_qty IS 'Quantity received so far (tracked via GRN)';
COMMENT ON COLUMN purchase_order_item.line_total IS 'Calculated: orderedQty * unitPrice';



-- Create grn_status enum
CREATE TYPE grn_status AS ENUM (
    'PENDING_QA',
    'APPROVED',
    'CANCELLED'
);

-- Create goods_receipt table
CREATE TABLE goods_receipt (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- GRN specific fields
    purchase_order_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    grn_number VARCHAR(100) UNIQUE,
    received_date BIGINT,
    supplier_invoice_no VARCHAR(100),
    status grn_status,

    -- Foreign key constraint
    CONSTRAINT fk_grn_purchase_order FOREIGN KEY (purchase_order_id)
        REFERENCES purchase_order(id) ON DELETE RESTRICT
);

-- Create goods_receipt_item table
CREATE TABLE goods_receipt_item (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- GRN item specific fields
    goods_receipt_id BIGINT NOT NULL,
    po_item_id BIGINT,
    item_id BIGINT NOT NULL,
    received_qty INTEGER NOT NULL,
    accepted_qty INTEGER NOT NULL,
    rejected_qty INTEGER NOT NULL,
    batch_number VARCHAR(100),
    expiry_date BIGINT,

    -- Foreign key constraint
    CONSTRAINT fk_grn_item_goods_receipt FOREIGN KEY (goods_receipt_id)
        REFERENCES goods_receipt(id) ON DELETE CASCADE,
    CONSTRAINT fk_grn_item_po_item FOREIGN KEY (po_item_id)
        REFERENCES purchase_order_item(id) ON DELETE SET NULL
);

-- Create indexes for goods_receipt
CREATE INDEX idx_goods_receipt_uuid ON goods_receipt(uuid);
CREATE INDEX idx_goods_receipt_purchase_order_id ON goods_receipt(purchase_order_id);
CREATE INDEX idx_goods_receipt_tenant_id ON goods_receipt(tenant_id);
CREATE INDEX idx_goods_receipt_grn_number ON goods_receipt(grn_number);
CREATE INDEX idx_goods_receipt_status ON goods_receipt(status);
CREATE INDEX idx_goods_receipt_received_date ON goods_receipt(received_date);
CREATE INDEX idx_goods_receipt_supplier_invoice_no ON goods_receipt(supplier_invoice_no);
CREATE INDEX idx_goods_receipt_is_deleted ON goods_receipt(is_deleted);
CREATE INDEX idx_goods_receipt_created_at ON goods_receipt(created_at);
CREATE INDEX idx_goods_receipt_updated_at ON goods_receipt(updated_at);

-- Composite indexes for common queries
CREATE INDEX idx_goods_receipt_tenant_status ON goods_receipt(tenant_id, status)
    WHERE is_deleted = false;
CREATE INDEX idx_goods_receipt_po_status ON goods_receipt(purchase_order_id, status)
    WHERE is_deleted = false;

-- Create indexes for goods_receipt_item
CREATE INDEX idx_grn_item_uuid ON goods_receipt_item(uuid);
CREATE INDEX idx_grn_item_goods_receipt_id ON goods_receipt_item(goods_receipt_id);
CREATE INDEX idx_grn_item_po_item_id ON goods_receipt_item(po_item_id);
CREATE INDEX idx_grn_item_item_id ON goods_receipt_item(item_id);
CREATE INDEX idx_grn_item_batch_number ON goods_receipt_item(batch_number);
CREATE INDEX idx_grn_item_expiry_date ON goods_receipt_item(expiry_date);
CREATE INDEX idx_grn_item_is_deleted ON goods_receipt_item(is_deleted);
CREATE INDEX idx_grn_item_created_at ON goods_receipt_item(created_at);
CREATE INDEX idx_grn_item_updated_at ON goods_receipt_item(updated_at);

-- Composite index for expiry tracking
CREATE INDEX idx_grn_item_expiry_tracking ON goods_receipt_item(item_id, batch_number, expiry_date)
    WHERE is_deleted = false;

-- Add table and column comments
COMMENT ON TABLE goods_receipt IS 'Goods Receipt Notes (GRN) for tracking received inventory';
COMMENT ON TABLE goods_receipt_item IS 'Line items for goods receipts with quality control data';

COMMENT ON COLUMN goods_receipt.grn_number IS 'Unique GRN number (e.g., GRN-2023-999)';
COMMENT ON COLUMN goods_receipt.supplier_invoice_no IS 'Invoice number from supplier';
COMMENT ON COLUMN goods_receipt.status IS 'Quality control and approval status';
COMMENT ON COLUMN goods_receipt.received_date IS 'Unix timestamp when goods were received';

COMMENT ON COLUMN goods_receipt_item.received_qty IS 'Physical count received';
COMMENT ON COLUMN goods_receipt_item.accepted_qty IS 'Quantity accepted after QA (received - rejected)';
COMMENT ON COLUMN goods_receipt_item.rejected_qty IS 'Damaged/defective items';
COMMENT ON COLUMN goods_receipt_item.batch_number IS 'Batch/lot number for tracking';
COMMENT ON COLUMN goods_receipt_item.expiry_date IS 'Unix timestamp of expiry date';



-- Create return_status enum
CREATE TYPE return_status AS ENUM (
    'DRAFT',
    'COMPLETED'
);

-- Create purchase_return table
CREATE TABLE purchase_return (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Purchase return specific fields
    tenant_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    goods_receipt_id BIGINT,
    supplier_id BIGINT NOT NULL,
    return_date BIGINT,
    reason TEXT,
    status return_status,

    -- Foreign key constraint
    CONSTRAINT fk_return_goods_receipt FOREIGN KEY (goods_receipt_id)
        REFERENCES goods_receipt(id) ON DELETE SET NULL
);

-- Create purchase_return_item table
CREATE TABLE purchase_return_item (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Purchase return item specific fields
    purchase_return_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    batch_number VARCHAR(100) NOT NULL,
    return_qty INTEGER NOT NULL,
    refund_price DECIMAL(18, 2),

    -- Foreign key constraint
    CONSTRAINT fk_return_item_purchase_return FOREIGN KEY (purchase_return_id)
        REFERENCES purchase_return(id) ON DELETE CASCADE
);

-- Create indexes for purchase_return
CREATE INDEX idx_purchase_return_uuid ON purchase_return(uuid);
CREATE INDEX idx_purchase_return_tenant_id ON purchase_return(tenant_id);
CREATE INDEX idx_purchase_return_warehouse_id ON purchase_return(warehouse_id);
CREATE INDEX idx_purchase_return_goods_receipt_id ON purchase_return(goods_receipt_id);
CREATE INDEX idx_purchase_return_supplier_id ON purchase_return(supplier_id);
CREATE INDEX idx_purchase_return_return_date ON purchase_return(return_date);
CREATE INDEX idx_purchase_return_status ON purchase_return(status);
CREATE INDEX idx_purchase_return_is_deleted ON purchase_return(is_deleted);
CREATE INDEX idx_purchase_return_created_at ON purchase_return(created_at);
CREATE INDEX idx_purchase_return_updated_at ON purchase_return(updated_at);

-- Composite indexes for common queries
CREATE INDEX idx_purchase_return_tenant_status ON purchase_return(tenant_id, status)
    WHERE is_deleted = false;
CREATE INDEX idx_purchase_return_supplier_date ON purchase_return(supplier_id, return_date)
    WHERE is_deleted = false;

-- Create indexes for purchase_return_item
CREATE INDEX idx_return_item_uuid ON purchase_return_item(uuid);
CREATE INDEX idx_return_item_purchase_return_id ON purchase_return_item(purchase_return_id);
CREATE INDEX idx_return_item_item_id ON purchase_return_item(item_id);
CREATE INDEX idx_return_item_batch_number ON purchase_return_item(batch_number);
CREATE INDEX idx_return_item_is_deleted ON purchase_return_item(is_deleted);
CREATE INDEX idx_return_item_created_at ON purchase_return_item(created_at);
CREATE INDEX idx_return_item_updated_at ON purchase_return_item(updated_at);

-- Composite index for batch tracking
CREATE INDEX idx_return_item_batch_tracking ON purchase_return_item(item_id, batch_number)
    WHERE is_deleted = false;

-- Add table and column comments
COMMENT ON TABLE purchase_return IS 'Purchase returns to suppliers';
COMMENT ON TABLE purchase_return_item IS 'Line items for purchase returns with batch tracking';

COMMENT ON COLUMN purchase_return.goods_receipt_id IS 'Optional link to original GRN';
COMMENT ON COLUMN purchase_return.return_date IS 'Unix timestamp when return was initiated';
COMMENT ON COLUMN purchase_return.status IS 'Return processing status';

COMMENT ON COLUMN purchase_return_item.batch_number IS 'Batch number being returned';
COMMENT ON COLUMN purchase_return_item.return_qty IS 'Quantity being returned to supplier';
COMMENT ON COLUMN purchase_return_item.refund_price IS 'Price per unit for refund calculation';
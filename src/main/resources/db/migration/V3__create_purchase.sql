-- Create purchase_request table (Header)
CREATE TABLE purchase_request (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Purchase request specific fields
    tenant_id BIGINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    requested_by_user_id BIGINT,
    department VARCHAR(100),
    prq_number VARCHAR(100) UNIQUE NOT NULL,
    status VARCHAR(50),
    total_estimated_amount DECIMAL(18, 2),
    notes TEXT
);

-- Create purchase_request_item table (Lines)
CREATE TABLE purchase_request_item (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Purchase request item specific fields
    purchase_request_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    requested_qty INTEGER NOT NULL,
    estimated_unit_price DECIMAL(18, 2),
    line_total DECIMAL(18, 2),

    -- Foreign key constraint linking to the header
    CONSTRAINT fk_prq_item_purchase_request FOREIGN KEY (purchase_request_id)
        REFERENCES purchase_request(id) ON DELETE CASCADE
);

-- Create indexes for performance and lookups
CREATE INDEX idx_purchase_request_uuid ON purchase_request(uuid);
CREATE INDEX idx_purchase_request_tenant_id ON purchase_request(tenant_id);
CREATE INDEX idx_purchase_request_status ON purchase_request(status);
CREATE INDEX idx_prq_item_purchase_request_id ON purchase_request_item(purchase_request_id);

-- Add table and column comments
COMMENT ON TABLE purchase_request IS 'Internal requests for purchasing goods';
COMMENT ON TABLE purchase_request_item IS 'Line items for internal purchase requests';
COMMENT ON COLUMN purchase_request.prq_number IS 'Unique PRQ number (e.g., PRQ-2026-001)';



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
    po_status VARCHAR(50),
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
CREATE INDEX idx_purchase_order_po_status ON purchase_order(po_status);
CREATE INDEX idx_po_item_purchase_order_id ON purchase_order_item(purchase_order_id);


-- Add table and column comments
COMMENT ON TABLE purchase_order IS 'Purchase orders for inventory procurement';
COMMENT ON TABLE purchase_order_item IS 'Line items for purchase orders';
COMMENT ON COLUMN purchase_order.order_number IS 'Unique PO number (e.g., PO-2023-001)';


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
    grn_status VARCHAR(50),

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

CREATE INDEX idx_goods_receipt_uuid ON goods_receipt(uuid);
CREATE INDEX idx_goods_receipt_purchase_order_id ON goods_receipt(purchase_order_id);
CREATE INDEX idx_goods_receipt_grn_status ON goods_receipt(grn_status);
CREATE INDEX idx_grn_item_goods_receipt_id ON goods_receipt_item(goods_receipt_id);
CREATE INDEX idx_grn_item_expiry_tracking ON goods_receipt_item(item_id, batch_number, expiry_date)
    WHERE is_deleted = false;


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
    pr_status VARCHAR(50),

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

CREATE INDEX idx_purchase_return_uuid ON purchase_return(uuid);
CREATE INDEX idx_purchase_return_pr_status ON purchase_return(pr_status); -- Updated index
CREATE INDEX idx_return_item_purchase_return_id ON purchase_return_item(purchase_return_id);

-- Add table and column comments
COMMENT ON TABLE purchase_return IS 'Purchase returns to suppliers';
COMMENT ON TABLE purchase_return_item IS 'Line items for purchase returns with batch tracking';
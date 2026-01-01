-- Create approval_type enum
CREATE TYPE approval_type AS ENUM (
    'HIGH_VALUE_INVOICE',
    'PO_APPROVAL',
    'STOCK_ADJUSTMENT',
    'SALES_ORDER_DISCOUNT',
    'INVOICE_DISCOUNT',
    'TAX_VARIANCE',
    'SALES_REFUND',
    'ADVANCE_REFUND'
);

-- Create approval_status enum
CREATE TYPE approval_status AS ENUM (
    'PENDING',
    'APPROVED',
    'REJECTED'
);

-- Create approval_result_status enum
CREATE TYPE approval_result_status AS ENUM (
    'AUTO_APPROVED',
    'APPROVAL_REQUIRED',
    'REJECTED'
);

-- Create approval_config table
CREATE TABLE approval_config (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Approval config specific fields
    tenant_id BIGINT NOT NULL,
    approval_type approval_type NOT NULL,
    is_enabled BOOLEAN DEFAULT true,
    threshold_amount DECIMAL(18, 2),
    threshold_percentage DOUBLE PRECISION,
    approver_role VARCHAR(100),

    -- Unique constraint: one config per approval type per tenant
    CONSTRAINT uk_approval_config_tenant_type UNIQUE (tenant_id, approval_type)
);

-- Create approval_request table
CREATE TABLE approval_request (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Approval request specific fields
    tenant_id BIGINT NOT NULL,
    approval_type approval_type NOT NULL,
    reference_id BIGINT NOT NULL,
    reference_code VARCHAR(100),
    status approval_status NOT NULL,
    requested_by BIGINT NOT NULL,
    description TEXT,
    value_amount DECIMAL(18, 2),
    actioned_by BIGINT,
    action_remarks TEXT
);

-- Create indexes for approval_config
CREATE INDEX idx_approval_config_uuid ON approval_config(uuid);
CREATE INDEX idx_approval_config_tenant_id ON approval_config(tenant_id);
CREATE INDEX idx_approval_config_approval_type ON approval_config(approval_type);
CREATE INDEX idx_approval_config_created_at ON approval_config(created_at);

-- Create indexes for approval_request
CREATE INDEX idx_approval_request_uuid ON approval_request(uuid);
CREATE INDEX idx_approval_request_tenant_id ON approval_request(tenant_id);
CREATE INDEX idx_approval_request_approval_type ON approval_request(approval_type);
CREATE INDEX idx_approval_request_reference_id ON approval_request(reference_id);
CREATE INDEX idx_approval_request_reference_code ON approval_request(reference_code);
CREATE INDEX idx_approval_request_status ON approval_request(status);
CREATE INDEX idx_approval_request_requested_by ON approval_request(requested_by);
CREATE INDEX idx_approval_request_actioned_by ON approval_request(actioned_by);
CREATE INDEX idx_approval_request_created_at ON approval_request(created_at);

-- Composite indexes for common queries
CREATE INDEX idx_approval_request_tenant_status ON approval_request(tenant_id, status)
    WHERE is_deleted = false;
CREATE INDEX idx_approval_request_tenant_type_status ON approval_request(tenant_id, approval_type, status)
    WHERE is_deleted = false;
CREATE INDEX idx_approval_request_type_reference ON approval_request(approval_type, reference_id)
    WHERE is_deleted = false;

-- Add table and column comments
COMMENT ON TABLE approval_config IS 'Configuration for approval rules per tenant and approval type';
COMMENT ON TABLE approval_request IS 'Approval requests tracking for various business operations';

COMMENT ON COLUMN approval_config.tenant_id IS 'Multi-tenant identifier';
COMMENT ON COLUMN approval_config.approval_type IS 'Type of approval: value-based, percentage-based, or absolute';
COMMENT ON COLUMN approval_config.is_enabled IS 'Whether this approval rule is active';
COMMENT ON COLUMN approval_config.threshold_amount IS 'Threshold amount for value-based approvals (e.g., Invoice > 10000)';
COMMENT ON COLUMN approval_config.threshold_percentage IS 'Threshold percentage for percentage-based approvals (e.g., Discount > 10%)';
COMMENT ON COLUMN approval_config.approver_role IS 'Role authorized to approve (e.g., MANAGER, ADMIN, SUPERVISOR)';

COMMENT ON COLUMN approval_request.tenant_id IS 'Multi-tenant identifier';
COMMENT ON COLUMN approval_request.approval_type IS 'Type of approval requested';
COMMENT ON COLUMN approval_request.reference_id IS 'Primary key of the source entity (Invoice, PO, etc.)';
COMMENT ON COLUMN approval_request.reference_code IS 'Human-readable identifier (e.g., INV-2024-001)';
COMMENT ON COLUMN approval_request.status IS 'Current status: PENDING, APPROVED, or REJECTED';
COMMENT ON COLUMN approval_request.requested_by IS 'User ID who initiated the request';
COMMENT ON COLUMN approval_request.description IS 'Reason why approval was triggered';
COMMENT ON COLUMN approval_request.value_amount IS 'Specific value that triggered approval';
COMMENT ON COLUMN approval_request.actioned_by IS 'User ID who approved/rejected';
COMMENT ON COLUMN approval_request.action_remarks IS 'Comments from approver';
-- Create approval_config table
CREATE TABLE approval_config (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    tenant_id BIGINT NOT NULL,
    approval_type VARCHAR(50) NOT NULL,
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
    approval_request_number VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Approval request specific fields
    tenant_id BIGINT NOT NULL,

    approval_type VARCHAR(50) NOT NULL,

    reference_id BIGINT NOT NULL,
    reference_code VARCHAR(100),

    approval_status VARCHAR(50) NOT NULL,

    requested_by BIGINT NOT NULL,
    description TEXT,
    value_amount DECIMAL(18, 2),
    actioned_by BIGINT,
    action_remarks TEXT,
    approved_date TIMESTAMP
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

-- Updated index name and column for approval_status
CREATE INDEX idx_approval_request_approval_status ON approval_request(approval_status);

CREATE INDEX idx_approval_request_requested_by ON approval_request(requested_by);
CREATE INDEX idx_approval_request_actioned_by ON approval_request(actioned_by);
CREATE INDEX idx_approval_request_created_at ON approval_request(created_at);

-- Composite indexes for common queries (Updated with new column name)
CREATE INDEX idx_approval_request_tenant_status ON approval_request(tenant_id, approval_status)
    WHERE is_deleted = false;
CREATE INDEX idx_approval_request_tenant_type_status ON approval_request(tenant_id, approval_type, approval_status)
    WHERE is_deleted = false;
CREATE INDEX idx_approval_request_type_reference ON approval_request(approval_type, reference_id)
    WHERE is_deleted = false;

-- Add table and column comments
COMMENT ON TABLE approval_config IS 'Configuration for approval rules per tenant and approval type';
COMMENT ON TABLE approval_request IS 'Approval requests tracking for various business operations';

COMMENT ON COLUMN approval_config.approval_type IS 'Type of approval stored as string (e.g. HIGH_VALUE_INVOICE)';
COMMENT ON COLUMN approval_request.approval_status IS 'Current status: PENDING, APPROVED, or REJECTED';
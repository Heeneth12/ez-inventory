-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS inventory;

-- Set search path to inventory schema
SET search_path TO inventory;

-- Create ENUM type in inventory schema
CREATE TYPE inventory.item_type AS ENUM ('PRODUCT', 'SERVICE');

-- Create items table
CREATE TABLE inventory.items (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Item specific fields
    name VARCHAR(255) NOT NULL,
    tenant_id BIGINT NOT NULL,
    item_code VARCHAR(255) NOT NULL UNIQUE,
    sku VARCHAR(255),
    barcode VARCHAR(255),
    item_type inventory.item_type NOT NULL,
    image_url VARCHAR(500),
    category VARCHAR(255),
    unit_of_measure VARCHAR(50),
    brand VARCHAR(255),
    manufacturer VARCHAR(255),
    purchase_price DECIMAL(18, 2),
    selling_price DECIMAL(18, 2),
    mrp DECIMAL(18, 2),
    tax_percentage DECIMAL(5, 2),
    discount_percentage DECIMAL(5, 2),
    hsn_sac_code VARCHAR(50),
    description TEXT,
    is_active BOOLEAN DEFAULT true
);

-- Create indexes for better query performance
CREATE INDEX idx_items_uuid ON inventory.items(uuid);
CREATE INDEX idx_items_tenant_id ON inventory.items(tenant_id);
CREATE INDEX idx_items_item_code ON inventory.items(item_code);
CREATE INDEX idx_items_sku ON inventory.items(sku);
CREATE INDEX idx_items_barcode ON inventory.items(barcode);
CREATE INDEX idx_items_category ON inventory.items(category);
CREATE INDEX idx_items_is_active ON inventory.items(is_active);
CREATE INDEX idx_items_tenant_active ON inventory.items(tenant_id, is_active) WHERE is_deleted = false;
CREATE INDEX idx_items_created_at ON inventory.items(created_at);

-- Add comment to table
COMMENT ON TABLE inventory.items IS 'Stores inventory item master data';
COMMENT ON COLUMN inventory.items.uuid IS 'Unique identifier for external references';
COMMENT ON COLUMN inventory.items.is_deleted IS 'Soft delete flag';

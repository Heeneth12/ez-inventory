CREATE TYPE contact_type AS ENUM ('CUSTOMER', 'SUPPLIER', 'BOTH');
CREATE TYPE address_type AS ENUM ('BILLING', 'SHIPPING', 'OFFICE', 'HOME', 'OTHER');

-- Create contact table
CREATE TABLE contact (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Contact specific fields
    contact_code VARCHAR(255),
    tenant_id BIGINT NOT NULL,
    name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    gst_number VARCHAR(50),
    type contact_type,
    credit_days INTEGER,
    active BOOLEAN DEFAULT true
);

-- Create address table
CREATE TABLE address (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Address specific fields
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    route VARCHAR(255),
    area VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    pin_code VARCHAR(20),
    type address_type,
    contact_id BIGINT,

    -- Foreign key constraint
    CONSTRAINT fk_address_contact FOREIGN KEY (contact_id)
        REFERENCES contact(id) ON DELETE CASCADE
);

-- Create indexes for contact table
CREATE INDEX idx_contact_uuid ON contact(uuid);
CREATE INDEX idx_contact_tenant_id ON contact(tenant_id);
CREATE INDEX idx_contact_code ON contact(contact_code);
CREATE INDEX idx_contact_email ON contact(email);
CREATE INDEX idx_contact_phone ON contact(phone);
CREATE INDEX idx_contact_gst_number ON contact(gst_number);
CREATE INDEX idx_contact_type ON contact(type);
CREATE INDEX idx_contact_active ON contact(active);
CREATE INDEX idx_contact_is_deleted ON contact(is_deleted);
CREATE INDEX idx_contact_tenant_active ON contact(tenant_id, active) WHERE is_deleted = false;
CREATE INDEX idx_contact_created_at ON contact(created_at);
CREATE INDEX idx_contact_updated_at ON contact(updated_at);

-- Create indexes for address table
CREATE INDEX idx_address_uuid ON address(uuid);
CREATE INDEX idx_address_contact_id ON address(contact_id);
CREATE INDEX idx_address_type ON address(type);
CREATE INDEX idx_address_city ON address(city);
CREATE INDEX idx_address_state ON address(state);
CREATE INDEX idx_address_pin_code ON address(pin_code);
CREATE INDEX idx_address_is_deleted ON address(is_deleted);
CREATE INDEX idx_address_created_at ON address(created_at);
CREATE INDEX idx_address_updated_at ON address(updated_at);

-- Add comments
COMMENT ON TABLE contact IS 'Stores contact information for customers, suppliers, etc.';
COMMENT ON TABLE address IS 'Stores addresses associated with contacts';
COMMENT ON COLUMN contact.uuid IS 'Unique identifier for external references';
COMMENT ON COLUMN address.uuid IS 'Unique identifier for external references';
COMMENT ON COLUMN contact.is_deleted IS 'Soft delete flag';
COMMENT ON COLUMN address.is_deleted IS 'Soft delete flag';
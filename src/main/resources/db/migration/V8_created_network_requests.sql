-- Corrected Network Requests Table
CREATE TABLE IF NOT EXISTS network_requests (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,

    sender_tenant_id BIGINT NOT NULL,
    receiver_tenant_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    message TEXT,
    sender_business_name VARCHAR(255)
);

-- Indexing for performance in your getMyNetwork calls
CREATE INDEX idx_network_requests_sender ON network_requests(sender_tenant_id);
CREATE INDEX idx_network_requests_receiver ON network_requests(receiver_tenant_id);
CREATE INDEX idx_network_requests_status ON network_requests(status);
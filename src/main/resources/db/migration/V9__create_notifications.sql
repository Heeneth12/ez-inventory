CREATE TABLE IF NOT EXISTS notifications (
    -- CommonSerializable Fields
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,

    -- Notification Specific Fields
    title VARCHAR(255),
    message TEXT,
    type VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(255),
    is_read BOOLEAN DEFAULT FALSE
);

-- Indexing for performance
CREATE INDEX idx_notifications_target ON notifications(target_id, target_type);
CREATE INDEX idx_notifications_read_status ON notifications(is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,

    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    target_scope VARCHAR(50) NOT NULL,
    sent_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS notification_deliveries (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,

    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,

    notification_id BIGINT NOT NULL,
    recipient_ref VARCHAR(255) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    failure_reason TEXT,

    CONSTRAINT fk_notification
        FOREIGN KEY (notification_id)
        REFERENCES notifications(id)
        ON DELETE CASCADE
);


-- Notifications indexes
CREATE INDEX IF NOT EXISTS idx_notifications_type
ON notifications(type);

CREATE INDEX IF NOT EXISTS idx_notifications_target_scope
ON notifications(target_scope);

CREATE INDEX IF NOT EXISTS idx_notifications_created_at
ON notifications(created_at);

-- User notification feed
CREATE INDEX IF NOT EXISTS idx_delivery_recipient_channel
ON notification_deliveries(recipient_ref, channel);

-- Unread count (badge)
CREATE INDEX IF NOT EXISTS idx_delivery_unread
ON notification_deliveries(recipient_ref, is_read);

-- Analytics per notification
CREATE INDEX IF NOT EXISTS idx_delivery_notification_id
ON notification_deliveries(notification_id);



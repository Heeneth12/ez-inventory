-- Create Chat Conversations Table
CREATE TABLE chat_conversations (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    app_key VARCHAR(255),
    title VARCHAR(255),
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create Chat Messages Table
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Key Constraint matching the @ManyToOne relationship
    CONSTRAINT fk_chat_messages_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES chat_conversations (id)
        ON DELETE CASCADE
);

-- Index to quickly find all conversations for a specific tenant/user
CREATE INDEX idx_chat_conversations_tenant_id ON chat_conversations(tenant_id);

-- Index to quickly load messages for a conversation, sorted by time
CREATE INDEX idx_chat_messages_conversation_id ON chat_messages(conversation_id);
CREATE INDEX idx_chat_messages_timestamp ON chat_messages(timestamp);
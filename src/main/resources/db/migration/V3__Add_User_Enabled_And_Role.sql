-- User ban/unban and profile fields
ALTER TABLE users ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);

-- Message type column (entity field)
ALTER TABLE messages ADD COLUMN IF NOT EXISTS type VARCHAR(20) NOT NULL DEFAULT 'TEXT';

-- Per-user message hide (delete for me)
CREATE TABLE IF NOT EXISTS message_hidden_by (
    message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    username VARCHAR(50) NOT NULL,
    PRIMARY KEY (message_id, username)
);

CREATE INDEX IF NOT EXISTS idx_message_hidden_by_message_id ON message_hidden_by(message_id);

-- Role column may already exist from V1; ensure default
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';

CREATE INDEX IF NOT EXISTS idx_users_active ON users(active);
CREATE INDEX IF NOT EXISTS idx_users_active_role ON users(active, role);

UPDATE users SET role = 'ADMIN' WHERE username = 'admin' AND role = 'USER';

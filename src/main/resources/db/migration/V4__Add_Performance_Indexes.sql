-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_room_members_room_user ON room_members(room_id, user_id);

CREATE INDEX IF NOT EXISTS idx_messages_room_id_active
    ON messages(room_id, id DESC)
    WHERE deleted = false;

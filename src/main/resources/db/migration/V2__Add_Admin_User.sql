-- Add default admin user
-- Password: admin123 (BCrypt hashed)
INSERT INTO users (username, email, password, display_name, role, created_at, updated_at)
VALUES (
    'admin',
    'admin@nexuschat.com',
    '$2a$10$Z6qHVG5EY7aXP3HKJy0p4uX7LfOO3M/Tl.eZwx3oEm6N9jHEQXFGu',
    'System Administrator',
    'ADMIN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO NOTHING;

-- Add default public room
INSERT INTO rooms (name, description, type, creator_id, created_at, updated_at)
VALUES (
    'General',
    'Default public chat room for everyone',
    'PUBLIC',
    (SELECT id FROM users WHERE username = 'admin'),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT DO NOTHING;

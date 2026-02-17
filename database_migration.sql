-- =====================================================
-- BANK ACCOUNT MANAGEMENT - SECURITY UPDATE MIGRATION
-- Version: 1.0.0
-- Date: 2024
-- Description: Adds security features including roles,
--              audit logging, and account locking
-- =====================================================

-- Step 1: Add new columns to users table
ALTER TABLE users 
  ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT 'USER',
  ADD COLUMN IF NOT EXISTS account_locked BOOLEAN DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER DEFAULT 0;

-- Step 2: Create audit_logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    details TEXT,
    amount DOUBLE PRECISION,
    account_number VARCHAR(20),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    error_message TEXT
);

-- Step 3: Create indexes for audit_logs table (improves query performance)
CREATE INDEX IF NOT EXISTS idx_audit_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_status ON audit_logs(status);
CREATE INDEX IF NOT EXISTS idx_audit_account ON audit_logs(account_number);

-- Step 4: Update existing users to have default role
UPDATE users 
SET role = 'USER' 
WHERE role IS NULL;

-- Step 5: Set constraints
ALTER TABLE users ALTER COLUMN role SET NOT NULL;
ALTER TABLE users ALTER COLUMN account_locked SET NOT NULL;
ALTER TABLE users ALTER COLUMN failed_login_attempts SET NOT NULL;

-- =====================================================
-- OPTIONAL: Create an admin user (CHANGE CREDENTIALS!)
-- =====================================================
-- IMPORTANT: You need to generate a BCrypt hash for the password
-- You can use: https://bcrypt-generator.com/ or bcrypt CLI
-- The password below is hashed version of "AdminPass123"

-- INSERT INTO users (
--     username, 
--     password, 
--     email, 
--     first_name, 
--     last_name, 
--     address, 
--     phone_number, 
--     account_number, 
--     balance, 
--     role,
--     account_locked,
--     failed_login_attempts
-- ) VALUES (
--     'admin',
--     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5gyg7ScqN8O1u', -- AdminPass123
--     'admin@yourbank.com',
--     'System',
--     'Administrator',
--     'Head Office',
--     '+10000000000',
--     '012345678900000',
--     0.0,
--     'ADMIN',
--     FALSE,
--     0
-- ) ON CONFLICT (username) DO NOTHING;

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================
-- Run these to verify migration was successful

-- Check users table structure
-- SELECT column_name, data_type, is_nullable 
-- FROM information_schema.columns 
-- WHERE table_name = 'users';

-- Check audit_logs table exists
-- SELECT table_name 
-- FROM information_schema.tables 
-- WHERE table_name = 'audit_logs';

-- Check indexes
-- SELECT indexname, indexdef 
-- FROM pg_indexes 
-- WHERE tablename = 'audit_logs';

-- Count existing users with roles
-- SELECT role, COUNT(*) 
-- FROM users 
-- GROUP BY role;

-- =====================================================
-- ROLLBACK SCRIPT (Use only if you need to undo)
-- =====================================================
-- WARNING: This will delete all audit data!

-- DROP TABLE IF EXISTS audit_logs;
-- ALTER TABLE users DROP COLUMN IF EXISTS role;
-- ALTER TABLE users DROP COLUMN IF EXISTS account_locked;
-- ALTER TABLE users DROP COLUMN IF EXISTS failed_login_attempts;

-- =====================================================
-- MIGRATION COMPLETE
-- =====================================================
```
-- ==============================================================================
-- CareConnect EHR Platform - Database Setup & Initial Admin Seed
-- ==============================================================================
-- Purpose:
-- 1. Create the MySQL database 'careconnect'
-- 2. Define the 'users' table schema (equivalent to Hibernate auto-generated schema)
-- 3. Provide documented template for seeding an initial ADMIN account
-- ==============================================================================

-- 1. Create database with UTF-8 character encoding
CREATE DATABASE IF NOT EXISTS careconnect
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE careconnect;

-- 2. User identity table definition
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Documented SQL Seed for Initial ADMIN Account
-- IMPORTANT: NEVER commit plaintext passwords or hardcoded production passwords.
-- Generate a BCrypt hash (e.g. using BCrypt with 10-12 salt rounds or Spring CLI).
--
-- Example below demonstrates insertion where:
-- Email: admin@careconnect.com
-- Password: Replace '<BCRYPT_HASH_OF_YOUR_PASSWORD>' with your generated BCrypt hash
--
-- INSERT INTO users (email, password, role, enabled, created_at, updated_at)
-- VALUES (
--     'admin@careconnect.com',
--     '<BCRYPT_HASH_OF_YOUR_PASSWORD>',
--     'ADMIN',
--     1,
--     NOW(),
--     NOW()
-- );

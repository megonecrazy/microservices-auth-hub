-- =============================================
-- V1: Initial Schema for Auth Service
-- =============================================

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    role            VARCHAR(50)  NOT NULL DEFAULT 'ROLE_USER',
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    enabled         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Indexes for frequent lookups
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- OTP Tokens table
CREATE TABLE IF NOT EXISTS otp_tokens (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    otp_code    VARCHAR(6)   NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    purpose     VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Index for OTP lookups
CREATE INDEX IF NOT EXISTS idx_otp_email_purpose ON otp_tokens(email, purpose, verified);

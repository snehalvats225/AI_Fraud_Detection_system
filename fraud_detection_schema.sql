-- =====================================================================
-- AI-Powered Fraud Detection System - database setup
-- Open this file in MySQL Workbench (File > Open SQL Script) as root
-- and click the lightning Execute button once.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS fraud_detection_db CHARACTER SET utf8mb4;
USE fraud_detection_db;

-- Admins and customers. Passwords are stored as SHA-256(salt + password).
CREATE TABLE IF NOT EXISTS users (
    user_id        INT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(20)   NOT NULL UNIQUE,
    password_hash  CHAR(64)      NOT NULL,
    salt           CHAR(32)      NOT NULL,
    role           ENUM('ADMIN', 'CUSTOMER') NOT NULL,
    full_name      VARCHAR(60)   NOT NULL,
    email          VARCHAR(80),
    phone          CHAR(10),
    home_city      VARCHAR(40)   NOT NULL DEFAULT 'Delhi',
    balance        DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status         ENUM('ACTIVE', 'BLOCKED') NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;

-- Every transaction with its fraud scores, decision and the feature values the
-- model saw. label = 1 (fraud) / 0 (genuine) once known -> training feedback.
CREATE TABLE IF NOT EXISTS transactions (
    txn_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT           NOT NULL,
    txn_type       ENUM('UPI_PAYMENT', 'CARD_PAYMENT', 'ONLINE_PURCHASE', 'ATM_WITHDRAWAL', 'BANK_TRANSFER', 'REFUND') NOT NULL,
    payee          VARCHAR(80)   NOT NULL,
    amount         DECIMAL(15,2) NOT NULL,
    category       ENUM('GROCERY', 'UTILITIES', 'FOOD_DINING', 'FUEL', 'SHOPPING', 'TRAVEL', 'ELECTRONICS',
                        'MONEY_TRANSFER', 'GIFT_CARDS', 'GAMBLING', 'CRYPTO') NOT NULL,
    city           VARCHAR(40)   NOT NULL,
    device         ENUM('MOBILE_APP', 'UPI_APP', 'NET_BANKING', 'POS_TERMINAL', 'ATM') NOT NULL,
    txn_time       DATETIME      NOT NULL,
    risk_score     DECIMAL(5,4)  NOT NULL DEFAULT 0,
    rule_score     DECIMAL(5,4)  NOT NULL DEFAULT 0,
    stat_score     DECIMAL(5,4)  NOT NULL DEFAULT 0,
    ml_score       DECIMAL(5,4)  NOT NULL DEFAULT 0,
    decision       ENUM('APPROVED', 'FLAGGED', 'BLOCKED') NOT NULL,
    balance_after  DECIMAL(15,2) NOT NULL,
    features       VARCHAR(400)  NULL,
    label          TINYINT       NULL,
    model_version  INT           NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_txn_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    INDEX idx_txn_user_time (user_id, txn_time),
    INDEX idx_txn_decision (decision)
) ENGINE = InnoDB;

-- Suspicious transactions for the admin to review.
CREATE TABLE IF NOT EXISTS fraud_cases (
    case_id      INT AUTO_INCREMENT PRIMARY KEY,
    txn_id       BIGINT        NOT NULL UNIQUE,
    user_id      INT           NOT NULL,
    risk_score   DECIMAL(5,4)  NOT NULL,
    reasons      VARCHAR(1000) NOT NULL,
    status       ENUM('OPEN', 'CONFIRMED_FRAUD', 'FALSE_POSITIVE') NOT NULL DEFAULT 'OPEN',
    admin_notes  VARCHAR(300),
    reviewed_by  INT           NULL,
    reviewed_at  DATETIME      NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_case_txn      FOREIGN KEY (txn_id)      REFERENCES transactions(txn_id),
    CONSTRAINT fk_case_user     FOREIGN KEY (user_id)     REFERENCES users(user_id),
    CONSTRAINT fk_case_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(user_id),
    INDEX idx_case_status (status)
) ENGINE = InnoDB;

-- Customer notifications. user_response is feedback for the ML model.
CREATE TABLE IF NOT EXISTS alerts (
    alert_id       INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT          NOT NULL,
    txn_id         BIGINT       NOT NULL,
    severity       ENUM('INFO', 'MEDIUM', 'HIGH') NOT NULL,
    message        VARCHAR(400) NOT NULL,
    is_read        BOOLEAN      NOT NULL DEFAULT FALSE,
    user_response  ENUM('NONE', 'CONFIRMED_GENUINE', 'REPORTED_FRAUD') NOT NULL DEFAULT 'NONE',
    responded_at   DATETIME     NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alert_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_alert_txn  FOREIGN KEY (txn_id)  REFERENCES transactions(txn_id),
    INDEX idx_alert_user (user_id, is_read)
) ENGINE = InnoDB;

-- Detection settings (single row, edited from the Configuration screen).
CREATE TABLE IF NOT EXISTS detection_config (
    id                 INT PRIMARY KEY,
    alert_threshold    DECIMAL(4,3)  NOT NULL,
    block_threshold    DECIMAL(4,3)  NOT NULL,
    high_amount_limit  DECIMAL(15,2) NOT NULL,
    night_start        INT           NOT NULL,
    night_end          INT           NOT NULL,
    velocity_limit     INT           NOT NULL,
    rule_weight        DECIMAL(4,3)  NOT NULL,
    stat_weight        DECIMAL(4,3)  NOT NULL,
    ml_weight          DECIMAL(4,3)  NOT NULL,
    learning_rate      DECIMAL(8,5)  NOT NULL,
    epochs             INT           NOT NULL,
    l2_lambda          DECIMAL(8,5)  NOT NULL,
    updated_by         VARCHAR(60)   NOT NULL DEFAULT 'system',
    updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;

INSERT IGNORE INTO detection_config (id, alert_threshold, block_threshold, high_amount_limit, night_start, night_end,
    velocity_limit, rule_weight, stat_weight, ml_weight, learning_rate, epochs, l2_lambda)
VALUES (1, 0.450, 0.750, 50000.00, 0, 5, 5, 0.300, 0.250, 0.450, 0.30000, 400, 0.00100);

-- Every trained version of the ML model (weights stored as comma-separated numbers).
CREATE TABLE IF NOT EXISTS model_versions (
    version_id        INT AUTO_INCREMENT PRIMARY KEY,
    algorithm         VARCHAR(40)  NOT NULL,
    weights           TEXT         NOT NULL,
    bias              DOUBLE       NOT NULL,
    feature_means     TEXT         NOT NULL,
    feature_stds      TEXT         NOT NULL,
    training_samples  INT          NOT NULL,
    real_samples      INT          NOT NULL,
    tp                INT          NOT NULL,
    fp                INT          NOT NULL,
    tn                INT          NOT NULL,
    fn                INT          NOT NULL,
    learning_rate     DOUBLE       NOT NULL,
    epochs            INT          NOT NULL,
    l2_lambda         DOUBLE       NOT NULL,
    notes             VARCHAR(200),
    trained_by        VARCHAR(60)  NOT NULL,
    is_active         BOOLEAN      NOT NULL DEFAULT FALSE,
    trained_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;

-- Default admin ->  username: admin   password: admin123
INSERT IGNORE INTO users (username, password_hash, salt, role, full_name, home_city, balance)
VALUES ('admin', '12cb3ec6af4a16a1ee8424dcc5a4cd51725c7bcbd9b3a5a5563150a55826c330', '5b8e2f1a9c3d7e4b6a0f2c8d1e9b3a7c', 'ADMIN', 'Fraud Analyst', 'Delhi', 0);

-- Demo customers -> password for all of them: user123  (balance Rs. 5,00,000 each)
INSERT IGNORE INTO users (username, password_hash, salt, role, full_name, email, phone, home_city, balance) VALUES
('rahul', 'fdc1798bd9f3dc38d65d1fb1567fbfcebb3fd92f3a702962a05e547e472d06de', 'b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6', 'CUSTOMER', 'Rahul Pratap Singh', 'rahul@example.com', '9876543210', 'Greater Noida', 500000.00),
('priya', 'e35af92488cef84e3369907845f701c322f389c9d58d36b602e8b95ab25b06da', 'c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7', 'CUSTOMER', 'Priya Sharma', 'priya@example.com', '9123456780', 'Delhi', 500000.00),
('amit', 'dd230f210f061d1b4261a490922a76fb5047fb6869e963213348bb1127d67273', 'd3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8', 'CUSTOMER', 'Amit Verma', 'amit@example.com', '9988776655', 'Mumbai', 500000.00),
('sneha', 'c5583795a8ac02352f3b5f813842464f4f54bd5ba527c131838d6bf3444a2e20', 'e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9', 'CUSTOMER', 'Sneha Iyer', 'sneha@example.com', '9090909090', 'Bengaluru', 500000.00),
('vikram', '3075ec74c017d36292bc364ef8f2fa0a0db0616574e79419721f50dbe4e9faa4', 'f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0', 'CUSTOMER', 'Vikram Rao', 'vikram@example.com', '8899001122', 'Hyderabad', 500000.00);

-- MySQL user that the application connects with (see src/db.properties)
CREATE USER IF NOT EXISTS 'frauduser'@'localhost' IDENTIFIED BY 'fraud123';
GRANT SELECT, INSERT, UPDATE, DELETE ON fraud_detection_db.* TO 'frauduser'@'localhost';
FLUSH PRIVILEGES;

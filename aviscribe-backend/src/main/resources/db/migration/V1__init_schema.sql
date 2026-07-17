CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    phone VARCHAR(32) DEFAULT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'USER',
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_name VARCHAR(255) NOT NULL,
    source_type INT NOT NULL DEFAULT 1,
    video_url VARCHAR(1024) DEFAULT NULL,
    video_local_path VARCHAR(512) DEFAULT NULL,
    audio_local_path VARCHAR(512) DEFAULT NULL,
    task_status INT NOT NULL DEFAULT 1,
    raw_text LONGTEXT DEFAULT NULL,
    formatted_text LONGTEXT DEFAULT NULL,
    error_log TEXT DEFAULT NULL,
    duration_seconds INT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    finish_time DATETIME DEFAULT NULL,
    INDEX idx_create_time (create_time),
    INDEX idx_task_status (task_status),
    INDEX idx_user_id (user_id),
    CONSTRAINT fk_task_user FOREIGN KEY (user_id) REFERENCES t_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    phone VARCHAR(32),
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    role VARCHAR(32) NOT NULL,
    status TINYINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_user_username ON t_user(username);
CREATE UNIQUE INDEX uk_user_phone ON t_user(phone);

CREATE TABLE IF NOT EXISTS t_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_name VARCHAR(255) NOT NULL,
    source_type INT NOT NULL,
    video_url VARCHAR(1024),
    video_local_path VARCHAR(512),
    audio_local_path VARCHAR(512),
    task_status INT NOT NULL,
    raw_text CLOB,
    formatted_text CLOB,
    error_log CLOB,
    duration_seconds INT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    finish_time TIMESTAMP,
    CONSTRAINT fk_task_user FOREIGN KEY (user_id) REFERENCES t_user(id)
);

CREATE INDEX idx_task_user ON t_task(user_id);
CREATE INDEX idx_task_status ON t_task(task_status);

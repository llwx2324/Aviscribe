-- Aviscribe 数据库初始化脚本

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS aviscribe_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE aviscribe_db;

-- 2. 创建用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '登录用户名',
    phone VARCHAR(32) DEFAULT NULL COMMENT '手机号',
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt 密码',
    display_name VARCHAR(128) NOT NULL COMMENT '展示名',
    role VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN/USER',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 3. 创建任务表（与用户关联）
CREATE TABLE IF NOT EXISTS t_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '任务ID',
    user_id BIGINT NOT NULL COMMENT '所属用户',
    task_name VARCHAR(255) NOT NULL COMMENT '任务名称',
    source_type INT NOT NULL DEFAULT 1 COMMENT '来源类型：1-本地上传，2-URL',
    video_url VARCHAR(1024) DEFAULT NULL COMMENT '视频URL（source_type=2时使用）',
    video_local_path VARCHAR(512) DEFAULT NULL COMMENT '视频本地路径',
    audio_local_path VARCHAR(512) DEFAULT NULL COMMENT '音频本地路径',
    task_status INT NOT NULL DEFAULT 1 COMMENT '任务状态：1-待处理，2-下载中，3-音频提取中，4-转录中，5-排版中，6-已完成，7-失败',
    raw_text LONGTEXT DEFAULT NULL COMMENT '原始转录文本',
    formatted_text LONGTEXT DEFAULT NULL COMMENT '格式化后的文本',
    error_log TEXT DEFAULT NULL COMMENT '错误日志',
    duration_seconds INT DEFAULT NULL COMMENT '音频时长（秒）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    finish_time DATETIME DEFAULT NULL COMMENT '完成时间',
    INDEX idx_create_time (create_time),
    INDEX idx_task_status (task_status),
    INDEX idx_user_id (user_id),
    CONSTRAINT fk_task_user FOREIGN KEY (user_id) REFERENCES t_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';


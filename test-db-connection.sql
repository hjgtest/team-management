-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS team_management_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE team_management_db;

-- 创建必要的表结构
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    role_id INT,
    team_id INT,
    gender VARCHAR(10),
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS teams (
    id INT AUTO_INCREMENT PRIMARY KEY,
    team_name VARCHAR(100) NOT NULL,
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_name VARCHAR(100) NOT NULL,
    description TEXT,
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS project_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    version_name VARCHAR(50) NOT NULL,
    start_time DATETIME,
    end_time DATETIME,
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS project_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    project_version_id BIGINT NOT NULL,
    assignment_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- 插入测试数据
INSERT INTO roles (role_name) VALUES ('管理员'), ('开发人员'), ('测试人员');
INSERT INTO teams (team_name) VALUES ('前端团队'), ('后端团队'), ('测试团队');
INSERT INTO users (username, phone, role_id, team_id, gender) VALUES 
('张三', '13800138001', 1, 1, '男'),
('李四', '13800138002', 2, 2, '女');
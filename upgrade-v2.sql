-- 升级 project_versions 表，添加创建时间和更新时间字段
USE team_management_db;

ALTER TABLE project_versions
    ADD COLUMN created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 为已有数据补充 created_time（使用 id 顺序作为近似创建时间）
UPDATE project_versions SET created_time = NOW() WHERE created_time IS NULL;

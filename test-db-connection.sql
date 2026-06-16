-- =====================================================================
-- 开发组管理系统 - 数据库建表脚本
-- 与代码实体类（src/main/java/com/teamdev/entity/）保持同步
-- 数据库: MySQL 5.7+ / 8.x
-- 字符集: utf8mb4
-- =====================================================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS team_management_db
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE team_management_db;

-- ---------------------------------------------------------------------
-- 用户表  对应 entity/User.java
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username  VARCHAR(50)  NOT NULL                COMMENT '用户名',
    phone     VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
    role_id   INT          DEFAULT NULL            COMMENT '角色ID（关联 roles.id）',
    team_id   INT          DEFAULT NULL            COMMENT '小组ID（关联 teams.id）',
    gender    VARCHAR(10)  DEFAULT NULL            COMMENT '性别',
    is_active TINYINT(1)   DEFAULT 1             COMMENT '是否在职：1=在职 0=离职',
    user_type INT          DEFAULT 2             COMMENT '用户类型：0=管理员 1=外派 2=正常研发成员',
    password  VARCHAR(255) DEFAULT '123456789'     COMMENT '登录密码',
    deleted   TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0=未删除 1=已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ---------------------------------------------------------------------
-- 角色表  对应 entity/Role.java
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_name        VARCHAR(50)   NOT NULL                COMMENT '角色名称',
    role_description VARCHAR(255)  DEFAULT NULL            COMMENT '角色描述',
    permissions      TEXT          DEFAULT NULL            COMMENT '权限列表',
    deleted          TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0=未删除 1=已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ---------------------------------------------------------------------
-- 小组表  对应 entity/Team.java
-- 备注：Team.createTime 在实体中类型为 String，故此处使用 VARCHAR
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS teams (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '小组ID',
    team_name   VARCHAR(100)  NOT NULL                COMMENT '小组名称',
    description VARCHAR(500)  DEFAULT NULL            COMMENT '小组描述',
    leader_id   BIGINT        DEFAULT NULL            COMMENT '负责人用户ID',
    create_time VARCHAR(50)   DEFAULT NULL            COMMENT '创建时间（字符串）',
    deleted     TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0=未删除 1=已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小组表';

-- ---------------------------------------------------------------------
-- 项目表  对应 entity/Project.java
-- 备注：status / versions 在实体中标注 exist=false，不入表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS projects (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '项目ID',
    project_name VARCHAR(100)  NOT NULL                COMMENT '项目名称',
    description  TEXT          DEFAULT NULL            COMMENT '项目描述',
    start_time   DATETIME      DEFAULT NULL            COMMENT '起始时间',
    end_time     DATETIME      DEFAULT NULL            COMMENT '结束时间',
    deleted      TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0=未删除 1=已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';

-- ---------------------------------------------------------------------
-- 项目版本表  对应 entity/ProjectVersion.java
-- 备注：status / project 在实体中标注 exist=false，不入表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS project_versions (
    id               BIGINT        NOT NULL AUTO_INCREMENT                                  COMMENT '版本ID',
    project_id       BIGINT        NOT NULL                                                 COMMENT '所属项目ID',
    version_name     VARCHAR(50)   NOT NULL                                                 COMMENT '版本名称',
    description      TEXT          DEFAULT NULL                                             COMMENT '版本描述',
    start_time       DATETIME      DEFAULT NULL                                             COMMENT '开始时间',
    end_time         DATETIME      DEFAULT NULL                                             COMMENT '结束时间',
    test_time        DATETIME      DEFAULT NULL                                             COMMENT '提测时间',
    launch_time      DATETIME      DEFAULT NULL                                             COMMENT '上线时间',
    created_time     DATETIME      DEFAULT CURRENT_TIMESTAMP                                COMMENT '创建时间（自动填充）',
    updated_time     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP    COMMENT '更新时间（自动填充）',
    product_manager  VARCHAR(100)  DEFAULT NULL                                             COMMENT '产品经理',
    prototype_url    VARCHAR(500)  DEFAULT NULL                                             COMMENT '原型链接',
    bug_count        INT           DEFAULT 0                                                COMMENT 'BUG 数量',
    is_delayed       TINYINT(1)    DEFAULT 0                                                COMMENT '是否延期：0=否 1=是',
    delayed_end_time DATETIME      DEFAULT NULL                                             COMMENT '延期后的结束时间',
    deleted          TINYINT       NOT NULL DEFAULT 0                                       COMMENT '逻辑删除：0=未删除 1=已删除',
    PRIMARY KEY (id),
    KEY idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目版本表';

-- ---------------------------------------------------------------------
-- 项目分配表（项目版本-用户-角色 关联）  对应 entity/ProjectAssignment.java
-- 备注：role_id 在实体中类型为 String，故此处使用 VARCHAR(64)（与 roles.id BIGINT 不同）
--       roleName 在实体中标注 exist=false，不入表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS project_assignments (
    id                  BIGINT        NOT NULL AUTO_INCREMENT                               COMMENT '主键',
    project_version_id  BIGINT        NOT NULL                                              COMMENT '项目版本ID',
    user_id             BIGINT        NOT NULL                                              COMMENT '用户ID',
    role_id             VARCHAR(64)   DEFAULT NULL                                          COMMENT '角色ID（字符串形式，业务自定义）',
    feature             VARCHAR(500)  DEFAULT NULL                                          COMMENT '负责的功能/特性',
    created_time        DATETIME      DEFAULT CURRENT_TIMESTAMP                             COMMENT '创建时间（自动填充）',
    updated_time        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（自动填充）',
    deleted             TINYINT       NOT NULL DEFAULT 0                                    COMMENT '逻辑删除：0=未删除 1=已删除',
    PRIMARY KEY (id),
    KEY idx_pv_user (project_version_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目分配表';

-- =====================================================================
-- 初始化测试数据（重复执行会产生重复行，按需手动跳过）
-- =====================================================================
INSERT INTO roles (role_name, role_description, permissions) VALUES
    ('管理员',   '系统管理员',   '*'),
    ('开发人员', '负责需求开发', 'project:dev'),
    ('测试人员', '负责测试',     'project:test');

INSERT INTO teams (team_name, description) VALUES
    ('前端团队', '负责前端开发'),
    ('后端团队', '负责后端开发'),
    ('测试团队', '负责测试');

INSERT INTO users (username, phone, role_id, team_id, gender) VALUES
    ('张三', '13800138001', 1, 1, '男'),
    ('李四', '13800138002', 2, 2, '女');

-- =====================================================================
-- 新增字段迁移（已存在的数据库执行以下语句）
-- =====================================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_active TINYINT(1) DEFAULT 1 COMMENT '是否在职：1=在职 0=离职';
ALTER TABLE users ADD COLUMN IF NOT EXISTS user_type INT DEFAULT 2 COMMENT '用户类型：0=管理员 1=外派 2=正常研发成员';
ALTER TABLE users ADD COLUMN IF NOT EXISTS password VARCHAR(255) DEFAULT '123456789' COMMENT '登录密码';
-- 为已有用户设置默认密码
UPDATE users SET password = '123456789' WHERE password IS NULL;

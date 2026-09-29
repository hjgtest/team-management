-- 站内通知表
CREATE TABLE IF NOT EXISTS `notifications` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `type` VARCHAR(50) NOT NULL COMMENT '通知类型：DEADLINE_WARNING-到期预警, SYSTEM-系统通知',
  `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
  `content` VARCHAR(500) DEFAULT '' COMMENT '通知内容',
  `project_id` BIGINT DEFAULT NULL COMMENT '关联项目ID',
  `version_id` BIGINT DEFAULT NULL COMMENT '关联版本ID',
  `urgency` INT DEFAULT 0 COMMENT '紧急程度：0-普通, 1-紧急, 2-非常紧急',
  `is_read` INT DEFAULT 0 COMMENT '是否已读：0-未读, 1-已读',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_type_read` (`type`, `is_read`),
  INDEX `idx_version_id` (`version_id`),
  INDEX `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知表';

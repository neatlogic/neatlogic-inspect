-- ----------------------------
-- Table structure for inspect_accessendpoint_script
-- ----------------------------
CREATE TABLE `inspect_accessendpoint_script` (
  `resource_id` bigint NOT NULL COMMENT '资源id',
  `script_id` bigint DEFAULT NULL COMMENT '脚本id',
  `config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '拓展配置',
  PRIMARY KEY (`resource_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产清单的脚本关联表';

-- ----------------------------
-- Table structure for inspect_alert_everyday
-- ----------------------------
CREATE TABLE `inspect_alert_everyday` (
  `report_time` date NOT NULL COMMENT '巡检时间',
  `resource_id` bigint NOT NULL COMMENT '资产ID',
  `alert_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '告警等级',
  `alert_object` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '告警对象',
  `alert_rule` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '告警规则',
  `alert_tips` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '告警提示',
  `alert_value` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '告警值',
  PRIMARY KEY (`resource_id`,`report_time`,`alert_object`) USING BTREE,
  KEY `idx_alertlevel` (`alert_level`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='巡检告警统计表';

-- ----------------------------
-- Table structure for inspect_appsystem_schedule
-- ----------------------------
CREATE TABLE `inspect_appsystem_schedule` (
  `id` bigint NOT NULL COMMENT 'id',
  `app_system_id` bigint NOT NULL COMMENT '应用ID',
  `cron` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'cron',
  `begin_time` timestamp(3) NULL DEFAULT NULL COMMENT '计划开始时间',
  `end_time` timestamp(3) NULL DEFAULT NULL COMMENT '计划结束时间',
  `is_active` tinyint(1) NOT NULL COMMENT '是否启用',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_app_system_id` (`app_system_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='定时巡检';

-- ----------------------------
-- Table structure for inspect_ci_combop
-- ----------------------------
CREATE TABLE `inspect_ci_combop` (
  `ci_id` bigint NOT NULL COMMENT 'ciType',
  `combop_id` bigint DEFAULT NULL COMMENT '组合工具id',
  PRIMARY KEY (`ci_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='巡检配置组合';

-- ----------------------------
-- Table structure for inspect_config_file_audit
-- ----------------------------
CREATE TABLE `inspect_config_file_audit` (
  `id` bigint NOT NULL COMMENT '唯一标识',
  `inspect_time` timestamp(3) NOT NULL COMMENT '巡检时间',
  `path_id` bigint NOT NULL COMMENT '配置文件路径id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_path_id` (`path_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='巡检资源配置文件记录';

-- ----------------------------
-- Table structure for inspect_config_file_last_change_time
-- ----------------------------
CREATE TABLE `inspect_config_file_last_change_time` (
  `resource_id` bigint NOT NULL COMMENT '资产id',
  `last_change_time` timestamp(3) NOT NULL COMMENT '最近变更时间',
  PRIMARY KEY (`resource_id`) USING BTREE,
  KEY `idx_last_change_time` (`last_change_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='巡检资产最近变更时间';

-- ----------------------------
-- Table structure for inspect_config_file_path
-- ----------------------------
CREATE TABLE `inspect_config_file_path` (
  `id` bigint NOT NULL COMMENT '唯一标识',
  `resource_id` bigint NOT NULL COMMENT '资源id',
  `path` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '路径',
  `md5` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最新配置文件MD5',
  `inspect_time` timestamp(3) NULL DEFAULT NULL COMMENT '最近修改时间',
  `file_id` bigint DEFAULT NULL COMMENT '文件id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_resource_id` (`resource_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='巡检资源配置文件路径';

-- ----------------------------
-- Table structure for inspect_config_file_version
-- ----------------------------
CREATE TABLE `inspect_config_file_version` (
  `id` bigint NOT NULL COMMENT '唯一标识',
  `md5` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'MD5',
  `inspect_time` timestamp(3) NOT NULL COMMENT '巡检时间',
  `file_id` bigint NOT NULL COMMENT '配置文件id',
  `audit_id` bigint NOT NULL COMMENT '巡检记录id',
  `path_id` bigint NOT NULL COMMENT '路径id',
  `job_id` bigint DEFAULT NULL COMMENT '作业ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_record_id` (`audit_id`) USING BTREE,
  KEY `idx_path_id` (`path_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='巡检资源配置文件版本';

-- ----------------------------
-- Table structure for inspect_new_problem_customview
-- ----------------------------
CREATE TABLE `inspect_new_problem_customview` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `user_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '所属用户',
  `condition_config` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '搜索条件配置',
  `sort` int NOT NULL COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_name_user` (`name`,`user_uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='巡检报告个人视图';

-- ----------------------------
-- Table structure for inspect_schedule
-- ----------------------------
CREATE TABLE `inspect_schedule` (
  `id` bigint NOT NULL COMMENT 'id',
  `uuid` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'uuid',
  `ci_id` bigint NOT NULL COMMENT '模型id',
  `cron` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'cron',
  `begin_time` timestamp(3) NULL DEFAULT NULL COMMENT '计划开始时间',
  `end_time` timestamp(3) NULL DEFAULT NULL COMMENT '计划结束时间',
  `is_active` tinyint(1) NOT NULL COMMENT '是否启用',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_ci_id` (`ci_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='定时巡检';
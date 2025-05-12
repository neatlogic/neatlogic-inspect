ALTER TABLE `inspect_alert_everyday`
    CHANGE `alert_rule` `alert_rule` VARCHAR (255) CHARSET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '告警规则';
ALTER TABLE `inspect_alert_everyday`
    CHANGE `alert_tips` `alert_tips` VARCHAR (255) CHARSET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '告警提示';


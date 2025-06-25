ALTER TABLE `inspect_alert_everyday` DROP PRIMARY KEY;

ALTER TABLE `inspect_alert_everyday` CHANGE `alert_object` `alert_object` TEXT NOT NULL COMMENT '告警对象';

ALTER TABLE `inspect_alert_everyday` ADD PRIMARY KEY (`resource_id`, `report_time`, `alert_object` (500));
/*
 * Copyright (C) 2025 TechSure Co., Ltd. All Rights Reserved.
 */
package neatlogic.module.inspect.job.source;

/**
 * 应用巡检作业来源类目的路由键生成与解析工具。
 */
public final class InspectAppJobRouteKey {

    public static final String APP_PREFIX = "app:";
    public static final String CI_PREFIX = "ci:";
    public static final String SCHEDULE_PREFIX = "schedule:";

    private InspectAppJobRouteKey() {
    }

    /**
     * 生成应用路由键。
     */
    public static String app(Long appSystemId) {
        return APP_PREFIX + appSystemId;
    }

    /**
     * 生成模型路由键。
     */
    public static String ci(Long ciId) {
        return CI_PREFIX + ciId;
    }

    /**
     * 生成应用巡检定时任务路由键。
     */
    public static String schedule(Long scheduleId) {
        return SCHEDULE_PREFIX + scheduleId;
    }

    /**
     * 解析指定类型路由键中的业务ID，不匹配时返回null。
     */
    public static Long parse(String routeKey, String prefix) {
        if (routeKey == null || !routeKey.startsWith(prefix)) {
            return null;
        }
        return Long.valueOf(routeKey.substring(prefix.length()));
    }
}

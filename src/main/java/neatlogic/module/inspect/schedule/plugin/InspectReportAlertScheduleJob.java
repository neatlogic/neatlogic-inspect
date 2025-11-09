/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.inspect.schedule.plugin;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.scheduler.core.JobBase;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.module.inspect.service.InspectReportService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2022/10/28 17:42
 **/
//@Component
@Deprecated
@DisallowConcurrentExecution
public class InspectReportAlertScheduleJob extends JobBase {
    private static final String CRON_EXPRESSION = "0 0 0 * * ?";//每天凌晨0点跑

    @Resource
    InspectReportService inspectReportService;

    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-INSPECT-REPORT-AlERT-SCHEDULE-JOB";
    }

    @Override
    public Boolean isMyHealthy(JobObject jobObject) {
        return true;
    }

    @Override
    public void reloadJob(JobObject jobObject) {
        String tenantUuid = jobObject.getTenantUuid();
        JobObject newJobObject = new JobObject.Builder("1", this.getGroupName(), this.getClassName(), tenantUuid).withCron(CRON_EXPRESSION).build();
        schedulerManager.loadJob(newJobObject);
    }

    @Override
    public void initJob(String tenantUuid) {
        JobObject newJobObject = new JobObject.Builder("1", this.getGroupName(), this.getClassName(), tenantUuid).withCron(CRON_EXPRESSION).build();
        schedulerManager.loadJob(newJobObject);
    }

    @Override
    public void executeInternal(JobExecutionContext context, JobObject jobObject) {
        inspectReportService.updateInspectAlertEveryDayData();
    }
}

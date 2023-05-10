/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.inspect.schedule.plugin;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.scheduler.core.JobBase;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.module.inspect.service.InspectReportService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2022/10/28 17:42
 **/
@Component
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

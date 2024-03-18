/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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

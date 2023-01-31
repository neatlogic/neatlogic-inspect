package neatlogic.module.inspect.api.schedule;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_SCHEDULE_EXECUTE;
import neatlogic.framework.inspect.dao.mapper.InspectScheduleMapper;
import neatlogic.framework.inspect.dto.InspectAppSystemScheduleVo;
import neatlogic.framework.inspect.exception.InspectAppSystemScheduleNotFoundException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.scheduler.core.IJob;
import neatlogic.framework.scheduler.core.SchedulerManager;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import neatlogic.module.inspect.schedule.plugin.InspectAppSystemScheduleJob;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = INSPECT_SCHEDULE_EXECUTE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class InspectAppSystemScheduleStatusUpdateApi extends PrivateApiComponentBase {

    @Resource
    private InspectScheduleMapper inspectScheduleMapper;

    @Resource
    private SchedulerManager schedulerManager;

    @Override
    public String getName() {
        return "启用/禁用巡检定时任务";
    }

    @Override
    public String getToken() {
        return "inspect/appsystem/schedule/status/update";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "任务id", isRequired = true),
            @Param(name = "isActive", type = ApiParamType.ENUM, rule = "0,1", desc = "是否启用", isRequired = true),
    })
    @Description(desc = "启用/禁用巡检定时任务")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        InspectAppSystemScheduleVo scheduleVo = inspectScheduleMapper.getInspectAppSystemScheduleById(id);
        if (scheduleVo == null) {
            throw new InspectAppSystemScheduleNotFoundException(id);
        }
        scheduleVo.setIsActive(paramObj.getInteger("isActive"));
        scheduleVo.setLcu(UserContext.get().getUserUuid());
        inspectScheduleMapper.updateInspectAppSystemScheduleStatus(scheduleVo);
        IJob jobHandler = SchedulerManager.getHandler(InspectAppSystemScheduleJob.class.getName());
        if (jobHandler == null) {
            throw new ScheduleHandlerNotFoundException(InspectAppSystemScheduleJob.class.getName());
        }
        String tenantUuid = TenantContext.get().getTenantUuid();
        JobObject jobObject = new JobObject.Builder(scheduleVo.getId().toString(), jobHandler.getGroupName(), jobHandler.getClassName(), tenantUuid)
                .withCron(scheduleVo.getCron()).withBeginTime(scheduleVo.getBeginTime())
                .withEndTime(scheduleVo.getEndTime())
                .setType("private")
                .build();
        if (scheduleVo.getIsActive() == 1) {
            schedulerManager.loadJob(jobObject);
        } else {
            schedulerManager.unloadJob(jobObject);
        }
        return null;
    }
}

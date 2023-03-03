package neatlogic.module.inspect.api.report;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.auth.INSPECT_SCHEDULE_EXECUTE;
import neatlogic.framework.inspect.dao.mapper.InspectScheduleMapper;
import neatlogic.framework.inspect.dto.InspectScheduleVo;
import neatlogic.framework.inspect.exception.InspectScheduleNotFoundException;
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
import neatlogic.framework.scheduler.exception.ScheduleIllegalParameterException;
import neatlogic.framework.util.SnowflakeUtil;
import neatlogic.module.inspect.schedule.plugin.InspectScheduleJob;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.quartz.CronExpression;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = INSPECT_SCHEDULE_EXECUTE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class InspectScheduleSaveApi extends PrivateApiComponentBase {

    @Resource
    private InspectScheduleMapper inspectScheduleMapper;

    @Resource
    private SchedulerManager schedulerManager;

    @Override
    public String getName() {
        return "保存巡检定时任务";
    }

    @Override
    public String getToken() {
        return "inspect/schedule/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "任务id"),
            @Param(name = "ciId", type = ApiParamType.LONG, desc = "模型ID", isRequired = true),
            @Param(name = "isActive", type = ApiParamType.ENUM, rule = "0,1", desc = "是否启用", isRequired = true),
            @Param(name = "cron", type = ApiParamType.STRING, desc = "cron", isRequired = true),
            @Param(name = "beginTime", type = ApiParamType.LONG, desc = "计划开始时间"),
            @Param(name = "endTime", type = ApiParamType.LONG, desc = "计划结束时间"),
    })
    @Description(desc = "保存巡检定时任务")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        InspectScheduleVo scheduleVo = JSON.toJavaObject(paramObj, InspectScheduleVo.class);
        if (!CronExpression.isValidExpression(scheduleVo.getCron())) {
            throw new ScheduleIllegalParameterException(scheduleVo.getCron());
        }
        if (scheduleVo.getId() != null) {
            InspectScheduleVo vo = inspectScheduleMapper.getInspectScheduleById(scheduleVo.getId());
            if (vo == null) {
                throw new InspectScheduleNotFoundException(scheduleVo.getId());
            }
            vo.setIsActive(scheduleVo.getIsActive());
            vo.setCron(scheduleVo.getCron());
            vo.setBeginTime(scheduleVo.getBeginTime());
            vo.setEndTime(scheduleVo.getEndTime());
            vo.setLcu(UserContext.get().getUserUuid());
            inspectScheduleMapper.updateInspectSchedule(vo);
            scheduleVo = vo;
        } else {
            ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
            if (ciCrossoverMapper.getCiById(scheduleVo.getCiId()) == null) {
                throw new CiNotFoundException(scheduleVo.getCiId());
            }
            scheduleVo.setId(SnowflakeUtil.uniqueLong());
            scheduleVo.setFcu(UserContext.get().getUserUuid());
            scheduleVo.setLcu(UserContext.get().getUserUuid());
            inspectScheduleMapper.insertInspectSchedule(scheduleVo);
        }
        IJob jobHandler = SchedulerManager.getHandler(InspectScheduleJob.class.getName());
        if (jobHandler == null) {
            throw new ScheduleHandlerNotFoundException(InspectScheduleJob.class.getName());
        }
        String tenantUuid = TenantContext.get().getTenantUuid();
        JobObject jobObject = new JobObject.Builder(scheduleVo.getUuid(), jobHandler.getGroupName(), jobHandler.getClassName(), tenantUuid)
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

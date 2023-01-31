/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.inspect.schedule.plugin;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.crossover.IAutoexecJobActionCrossoverService;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.filter.core.LoginAuthHandlerBase;
import neatlogic.framework.inspect.constvalue.JobSource;
import neatlogic.framework.inspect.dao.mapper.InspectMapper;
import neatlogic.framework.inspect.dao.mapper.InspectScheduleMapper;
import neatlogic.framework.inspect.dto.InspectScheduleVo;
import neatlogic.framework.scheduler.core.JobBase;
import neatlogic.framework.scheduler.dto.JobObject;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @author laiwt
 * @since 2021/12/07 17:42
 **/
@Component
@DisallowConcurrentExecution
public class InspectScheduleJob extends JobBase {

    @Resource
    InspectScheduleMapper inspectScheduleMapper;

    @Resource
    InspectMapper inspectMapper;

    @Resource
    UserMapper userMapper;

    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-INSPECT-SCHEDULE-JOB";
    }

    @Override
    public Boolean isMyHealthy(JobObject jobObject) {
        String uuid = jobObject.getJobName();
        InspectScheduleVo inspectScheduleVo = inspectScheduleMapper.getInspectScheduleByUuid(uuid);
        if (inspectScheduleVo == null) {
            return false;
        }
        return Objects.equals(inspectScheduleVo.getIsActive(), 1) && Objects.equals(inspectScheduleVo.getCron(), jobObject.getCron());
    }

    @Override
    public void reloadJob(JobObject jobObject) {
        String tenantUuid = jobObject.getTenantUuid();
        TenantContext.get().switchTenant(tenantUuid);
        String uuid = jobObject.getJobName();
        InspectScheduleVo inspectScheduleVo = inspectScheduleMapper.getInspectScheduleByUuid(uuid);
        if (inspectScheduleVo != null) {
            JobObject newJobObjectBuilder = new JobObject.Builder(inspectScheduleVo.getUuid(), this.getGroupName(), this.getClassName(), tenantUuid)
                    .withCron(inspectScheduleVo.getCron()).withBeginTime(inspectScheduleVo.getBeginTime())
                    .withEndTime(inspectScheduleVo.getEndTime())
                    .build();
            schedulerManager.loadJob(newJobObjectBuilder);
        }

    }

    @Override
    public void initJob(String tenantUuid) {
        List<InspectScheduleVo> list = inspectScheduleMapper.getInspectScheduleList();
        list.removeIf(o -> !Objects.equals(o.getIsActive(), 1));
        for (InspectScheduleVo vo : list) {
            JobObject.Builder jobObjectBuilder = new JobObject
                    .Builder(vo.getUuid(), this.getGroupName(), this.getClassName(), TenantContext.get().getTenantUuid());
            JobObject jobObject = jobObjectBuilder.build();
            this.reloadJob(jobObject);
        }
    }

    @Override
    public void executeInternal(JobExecutionContext context, JobObject jobObject) throws Exception {
        String uuid = jobObject.getJobName();
        InspectScheduleVo inspectScheduleVo = inspectScheduleMapper.getInspectScheduleByUuid(uuid);
        Long combopId = inspectMapper.getCombopIdByCiId(inspectScheduleVo.getCiId());
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo ci = ciCrossoverMapper.getCiById(inspectScheduleVo.getCiId());
        if (combopId != null && ci != null) {
            JSONObject paramObj = new JSONObject();
            paramObj.put("combopId", combopId);
            paramObj.put("source", JobSource.SCHEDULE_INSPECT.getValue());
            paramObj.put("operationId", combopId);
            paramObj.put("invokeId", inspectScheduleVo.getId());
            paramObj.put("isFirstFire", 1);
            paramObj.put("operationType", CombopOperationType.COMBOP.getValue());
            paramObj.put("name", ci.getLabel() + (ci.getName() != null ? "(" + ci.getName() + ")" : StringUtils.EMPTY) + " 巡检");
            JSONObject executeConfig = new JSONObject();
            executeConfig.put("executeNodeConfig", new JSONObject() {
                {
                    this.put("filter", new JSONObject() {
                        {
                            this.put("typeIdList", new JSONArray() {
                                {
                                    this.add(ci.getId());
                                }
                            });
                        }
                    });
                }
            });
            paramObj.put("executeConfig", executeConfig);
            UserVo fcuVo = userMapper.getUserByUuid(inspectScheduleVo.getFcu());
            UserContext.init(fcuVo, SystemUser.SYSTEM.getTimezone());
            UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(fcuVo).getCc());
            IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
            AutoexecJobVo jobVo = JSONObject.toJavaObject(paramObj, AutoexecJobVo.class);
            autoexecJobActionCrossoverService.validateAndCreateJobFromCombop(jobVo);
            jobVo.setAction(JobAction.FIRE.getValue());
            IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
            fireAction.doService(jobVo);
        }
    }
}

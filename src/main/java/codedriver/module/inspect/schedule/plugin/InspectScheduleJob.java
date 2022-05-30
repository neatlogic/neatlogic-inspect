/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.schedule.plugin;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.crossover.IAutoexecJobActionCrossoverService;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.cmdb.crossover.ICiCrossoverMapper;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.common.constvalue.SystemUser;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.filter.core.LoginAuthHandlerBase;
import codedriver.framework.inspect.constvalue.JobSource;
import codedriver.framework.inspect.dao.mapper.InspectMapper;
import codedriver.framework.inspect.dao.mapper.InspectScheduleMapper;
import codedriver.framework.inspect.dto.InspectScheduleVo;
import codedriver.framework.scheduler.core.JobBase;
import codedriver.framework.scheduler.dto.JobObject;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.StringUtils;
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
    public Boolean isHealthy(JobObject jobObject) {
        return true;
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
            paramObj.put("source", JobSource.INSPECT.getValue());
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
            AutoexecJobVo jobVo = autoexecJobActionCrossoverService.validateCreateJobFromCombop(paramObj, false);
            jobVo.setAction(JobAction.FIRE.getValue());
            IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
            fireAction.doService(jobVo);
        }
    }
}

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
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.crossover.IAutoexecJobActionCrossoverService;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteNodeConfigVo;
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
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
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
            AutoexecJobVo jobVo = new AutoexecJobVo();
            jobVo.setOperationId(combopId);
            jobVo.setSource(JobSource.SCHEDULE_INSPECT.getValue());
            jobVo.setInvokeId(inspectScheduleVo.getId());
            jobVo.setRouteId(inspectScheduleVo.getId().toString());
            jobVo.setIsFirstFire(1);
            jobVo.setOperationType(CombopOperationType.COMBOP.getValue());
            jobVo.setName(ci.getLabel() + (ci.getName() != null ? "(" + ci.getName() + ")" : StringUtils.EMPTY) + " 巡检");
            AutoexecCombopExecuteConfigVo executeConfig = new AutoexecCombopExecuteConfigVo();
            AutoexecCombopExecuteNodeConfigVo executeNodeConfig = new AutoexecCombopExecuteNodeConfigVo();
            JSONObject filter = new JSONObject();
            List<Long> typeIdList = new ArrayList<>();
            typeIdList.add(ci.getId());
            filter.put("typeIdList", typeIdList);
            executeNodeConfig.setFilter(filter);
            executeConfig.setExecuteNodeConfig(executeNodeConfig);
            jobVo.setExecuteConfig(executeConfig);
            UserVo fcuVo = userMapper.getUserByUuid(inspectScheduleVo.getFcu());
            UserContext.init(fcuVo, SystemUser.SYSTEM.getTimezone());
            UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(fcuVo).getCc());
            IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
            autoexecJobActionCrossoverService.validateAndCreateJobFromCombop(jobVo);
            jobVo.setAction(JobAction.FIRE.getValue());
            IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
            fireAction.doService(jobVo);
        }
    }
}

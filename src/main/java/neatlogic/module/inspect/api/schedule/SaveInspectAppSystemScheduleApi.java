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

package neatlogic.module.inspect.api.schedule;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.auth.INSPECT_SCHEDULE_EXECUTE;
import neatlogic.framework.inspect.dao.mapper.InspectScheduleMapper;
import neatlogic.framework.inspect.dto.InspectAppSystemScheduleVo;
import neatlogic.framework.inspect.exception.InspectAppSystemScheduleNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.scheduler.core.IJob;
import neatlogic.framework.scheduler.core.SchedulerManager;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import neatlogic.framework.scheduler.exception.ScheduleIllegalParameterException;
import neatlogic.framework.util.SnowflakeUtil;
import neatlogic.module.inspect.schedule.plugin.InspectAppSystemScheduleJob;
import org.quartz.CronExpression;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = INSPECT_SCHEDULE_EXECUTE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveInspectAppSystemScheduleApi extends PrivateApiComponentBase {

    @Resource
    private InspectScheduleMapper inspectScheduleMapper;

    @Resource
    private SchedulerManager schedulerManager;

    @Override
    public String getToken() {
        return "inspect/appsystem/schedule/save";
    }
    @Override
    public String getName() {
        return "保存应用定时巡检任务";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "任务id"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用ID", isRequired = true),
            @Param(name = "isActive", type = ApiParamType.ENUM, rule = "0,1", desc = "是否启用", isRequired = true),
            @Param(name = "cron", type = ApiParamType.STRING, desc = "cron", isRequired = true),
            @Param(name = "beginTime", type = ApiParamType.LONG, desc = "计划开始时间"),
            @Param(name = "endTime", type = ApiParamType.LONG, desc = "计划结束时间"),
    })
    @Output({
            @Param(name = "id", type = ApiParamType.LONG, desc = "任务id")
    })
    @Description(desc = "保存巡检定时任务")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        InspectAppSystemScheduleVo scheduleVo = JSON.toJavaObject(paramObj, InspectAppSystemScheduleVo.class);
        if (!CronExpression.isValidExpression(scheduleVo.getCron())) {
            throw new ScheduleIllegalParameterException(scheduleVo.getCron());
        }
        Long appSystemId = scheduleVo.getAppSystemId();
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystem = resourceCrossoverMapper.getAppSystemById(appSystemId);
        if (appSystem == null) {
            throw new AppSystemNotFoundException(appSystemId);
        }
        if (scheduleVo.getId() != null) {
            InspectAppSystemScheduleVo vo = inspectScheduleMapper.getInspectAppSystemScheduleById(scheduleVo.getId());
            if (vo == null) {
                throw new InspectAppSystemScheduleNotFoundException(scheduleVo.getId());
            }
            scheduleVo.setLcu(UserContext.get().getUserUuid());
            inspectScheduleMapper.updateInspectAppSystemSchedule(scheduleVo);
        } else {
            scheduleVo.setId(SnowflakeUtil.uniqueLong());
            scheduleVo.setFcu(UserContext.get().getUserUuid());
            scheduleVo.setLcu(UserContext.get().getUserUuid());
            inspectScheduleMapper.insertInspectAppSystemSchedule(scheduleVo);
        }
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
        JSONObject resultObj = new JSONObject();
        resultObj.put("id", scheduleVo.getId());
        return resultObj;
    }
}

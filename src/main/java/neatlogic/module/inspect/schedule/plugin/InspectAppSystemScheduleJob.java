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
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.filter.core.LoginAuthHandlerBase;
import neatlogic.framework.inspect.constvalue.JobSource;
import neatlogic.framework.inspect.dao.mapper.InspectMapper;
import neatlogic.framework.inspect.dao.mapper.InspectScheduleMapper;
import neatlogic.framework.inspect.dto.InspectAppSystemScheduleVo;
import neatlogic.framework.scheduler.core.JobBase;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.service.AuthenticationInfoService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author laiwt
 * @since 2021/12/07 17:42
 **/
@Component
@DisallowConcurrentExecution
public class InspectAppSystemScheduleJob extends JobBase {

    private Logger logger = LoggerFactory.getLogger(InspectAppSystemScheduleJob.class);

    @Resource
    InspectScheduleMapper inspectScheduleMapper;

    @Resource
    InspectMapper inspectMapper;

    @Resource
    UserMapper userMapper;

    @Resource
    private AuthenticationInfoService authenticationInfoService;

    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-INSPECT-SCHEDULE-JOB";
    }

    @Override
    public Boolean isMyHealthy(JobObject jobObject) {
        String idStr = jobObject.getJobName();
        Long id = Long.parseLong(idStr);
        InspectAppSystemScheduleVo scheduleVo = inspectScheduleMapper.getInspectAppSystemScheduleById(id);
        if (scheduleVo == null) {
            return false;
        }
        return Objects.equals(scheduleVo.getIsActive(), 1) && Objects.equals(scheduleVo.getCron(), jobObject.getCron());
    }

    @Override
    public void reloadJob(JobObject jobObject) {
        String tenantUuid = jobObject.getTenantUuid();
        TenantContext.get().switchTenant(tenantUuid);
        String idStr = jobObject.getJobName();
        Long id = Long.parseLong(idStr);
        InspectAppSystemScheduleVo scheduleVo = inspectScheduleMapper.getInspectAppSystemScheduleById(id);
        if (scheduleVo != null) {
            JobObject newJobObjectBuilder = new JobObject.Builder(idStr, this.getGroupName(), this.getClassName(), tenantUuid)
                    .withCron(scheduleVo.getCron()).withBeginTime(scheduleVo.getBeginTime())
                    .withEndTime(scheduleVo.getEndTime())
                    .build();
            schedulerManager.loadJob(newJobObjectBuilder);
        }

    }

    @Override
    public void initJob(String tenantUuid) {
        InspectAppSystemScheduleVo searchVo = new InspectAppSystemScheduleVo();
        searchVo.setIsActive(1);
        int rowNum = inspectScheduleMapper.getInspectAppSystemScheduleCount(searchVo);
        if (rowNum == 0) {
            return;
        }
        searchVo.setRowNum(rowNum);
        searchVo.setPageSize(100);
        for (int currentPage = 1; currentPage <= searchVo.getPageCount(); currentPage++) {
            searchVo.setCurrentPage(currentPage);
            List<InspectAppSystemScheduleVo> list = inspectScheduleMapper.getInspectAppSystemScheduleList(searchVo);
            for (InspectAppSystemScheduleVo vo : list) {
                JobObject.Builder jobObjectBuilder = new JobObject
                        .Builder(vo.getId().toString(), this.getGroupName(), this.getClassName(), TenantContext.get().getTenantUuid());
                JobObject jobObject = jobObjectBuilder.build();
                this.reloadJob(jobObject);
            }
        }
    }

    @Override
    public void executeInternal(JobExecutionContext context, JobObject jobObject) throws Exception {
        String idStr = jobObject.getJobName();
        Long id = Long.parseLong(idStr);
        InspectAppSystemScheduleVo scheduleVo = inspectScheduleMapper.getInspectAppSystemScheduleById(id);
        if (scheduleVo == null) {
            return;
        }
        String userUuid = scheduleVo.getFcu();
        List<Long> ipObjectResourceTypeIdList = new ArrayList<>();
        List<Long> osResourceTypeIdList = new ArrayList<>();
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceSearchVo searchVo = new ResourceSearchVo();
        searchVo.setAppSystemId(scheduleVo.getAppSystemId());
        Set<Long> resourceTypeIdSet = resourceCrossoverMapper.getIpObjectResourceTypeIdListByAppSystemIdAndEnvId(searchVo);
        ipObjectResourceTypeIdList.addAll(resourceTypeIdSet);
        ipObjectResourceTypeIdList.sort(Long::compare);
        if (CollectionUtils.isNotEmpty(resourceTypeIdSet)) {
            resourceTypeIdSet = resourceCrossoverMapper.getOsResourceTypeIdListByAppSystemIdAndEnvId(searchVo);
            osResourceTypeIdList.addAll(resourceTypeIdSet);
            osResourceTypeIdList.sort(Long::compare);
        }
        for (Long typeId : ipObjectResourceTypeIdList) {
            Long combopId = inspectMapper.getCombopIdByCiId(typeId);
            if (combopId == null) {
                continue;
            }
            ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
            CiVo ci = ciCrossoverMapper.getCiById(typeId);
            if (ci == null) {
                continue;
            }
            String name = ci.getLabel() + (ci.getName() != null ? "(" + ci.getName() + ")" : StringUtils.EMPTY) + " 巡检";
            List<AutoexecNodeVo> selectNodeList = new ArrayList<>();
            searchVo.setTypeId(typeId);
            int rowNum = resourceCrossoverMapper.getIpObjectResourceCountByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                for (int currentPage = 1; currentPage <= searchVo.getPageCount(); currentPage++) {
                    searchVo.setCurrentPage(currentPage);
                    List<Long> idList = resourceCrossoverMapper.getIpObjectResourceIdListByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
                    if (CollectionUtils.isNotEmpty(idList)) {
                        List<ResourceVo> resourceList = resourceCrossoverMapper.getResourceByIdList(idList);
                        for (ResourceVo resourceVo : resourceList) {
                            selectNodeList.add(new AutoexecNodeVo(resourceVo));
                        }
                    }
                }
                try {
                    createAndFireJob(combopId, id, name, userUuid, selectNodeList);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        for (Long typeId : osResourceTypeIdList) {
            Long combopId = inspectMapper.getCombopIdByCiId(typeId);
            if (combopId == null) {
                continue;
            }
            ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
            CiVo ci = ciCrossoverMapper.getCiById(typeId);
            if (ci == null) {
                continue;
            }
            String name = ci.getLabel() + (ci.getName() != null ? "(" + ci.getName() + ")" : StringUtils.EMPTY) + " 巡检";
            List<AutoexecNodeVo> selectNodeList = new ArrayList<>();
            searchVo.setTypeId(typeId);
            int rowNum = resourceCrossoverMapper.getOsResourceCountByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                for (int currentPage = 1; currentPage <= searchVo.getPageCount(); currentPage++) {
                    searchVo.setCurrentPage(currentPage);
                    List<Long> idList = resourceCrossoverMapper.getOsResourceIdListByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
                    if (CollectionUtils.isNotEmpty(idList)) {
                        List<ResourceVo> resourceList = resourceCrossoverMapper.getResourceByIdList(idList);
                        for (ResourceVo resourceVo : resourceList) {
                            selectNodeList.add(new AutoexecNodeVo(resourceVo));
                        }
                    }
                }
                try {
                    createAndFireJob(combopId, id, name, userUuid, selectNodeList);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

    }

    private void createAndFireJob(Long combopId, Long invokeId, String name, String userUuid, List<AutoexecNodeVo> selectNodeList) throws Exception {
        AutoexecJobVo jobVo = new AutoexecJobVo();
        jobVo.setOperationId(combopId);
        jobVo.setSource(JobSource.SCHEDULE_INSPECT_APP.getValue());
        jobVo.setInvokeId(invokeId);
        jobVo.setRouteId(invokeId.toString());
        jobVo.setIsFirstFire(1);
        jobVo.setOperationType(CombopOperationType.COMBOP.getValue());
        jobVo.setName(name);
        AutoexecCombopExecuteNodeConfigVo executeNodeConfig = new AutoexecCombopExecuteNodeConfigVo();
        executeNodeConfig.setSelectNodeList(selectNodeList);
        AutoexecCombopExecuteConfigVo executeConfig = new AutoexecCombopExecuteConfigVo();
        executeConfig.setExecuteNodeConfig(executeNodeConfig);
        jobVo.setExecuteConfig(executeConfig);
        UserVo fcuVo = userMapper.getUserByUuid(userUuid);
        AuthenticationInfoVo authenticationInfoVo = authenticationInfoService.getAuthenticationInfo(userUuid);
        UserContext.init(fcuVo, authenticationInfoVo, SystemUser.SYSTEM.getTimezone());
        UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(fcuVo).getCc());
        IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
        autoexecJobActionCrossoverService.validateAndCreateJobFromCombop(jobVo);
        jobVo.setAction(JobAction.FIRE.getValue());
        IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
        fireAction.doService(jobVo);
    }
}

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
import codedriver.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopExecuteNodeConfigVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.dto.node.AutoexecNodeVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.cmdb.crossover.ICiCrossoverMapper;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.SystemUser;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.filter.core.LoginAuthHandlerBase;
import codedriver.framework.inspect.constvalue.JobSource;
import codedriver.framework.inspect.dao.mapper.InspectMapper;
import codedriver.framework.inspect.dao.mapper.InspectScheduleMapper;
import codedriver.framework.inspect.dto.InspectAppSystemScheduleVo;
import codedriver.framework.inspect.dto.InspectResourceVo;
import codedriver.framework.inspect.dto.InspectScheduleVo;
import codedriver.framework.scheduler.core.JobBase;
import codedriver.framework.scheduler.dto.JobObject;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
                System.out.println("loadJob:" + vo.getId().toString());
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
        System.out.println("executeInternal:" + idStr);
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
        System.out.println("ipObjectResourceTypeIdList=" + ipObjectResourceTypeIdList);
        for (Long typeId : ipObjectResourceTypeIdList) {
            System.out.println("typeId=" + typeId);
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
                        System.out.println("idList=" + idList);
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
                    System.out.println(e.getMessage());
                }
            }
        }
        System.out.println("osResourceTypeIdList=" + osResourceTypeIdList);
        for (Long typeId : osResourceTypeIdList) {
            System.out.println("typeId=" + typeId);
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
                        System.out.println("idList=" + idList);
                        List<ResourceVo> resourceList = resourceCrossoverMapper.getResourceByIdList(idList);
                        for (ResourceVo resourceVo : resourceList) {
                            selectNodeList.add(new AutoexecNodeVo(resourceVo));
                        }
                    }
                }
//                createAndFireJob(combopId, id, name, userUuid, selectNodeList);
                try {
                    createAndFireJob(combopId, id, name, userUuid, selectNodeList);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    System.out.println(e.getMessage());
                }
            }
        }

    }

    private void createAndFireJob(Long combopId, Long invokeId, String name, String userUuid, List<AutoexecNodeVo> selectNodeList) throws Exception {
        System.out.println("createAndFireJob=" + combopId);
        JSONObject paramObj = new JSONObject();
        paramObj.put("combopId", combopId);
        paramObj.put("source", JobSource.INSPECT.getValue());
        paramObj.put("operationId", combopId);
        paramObj.put("invokeId", invokeId);
        paramObj.put("isFirstFire", 1);
        paramObj.put("operationType", CombopOperationType.COMBOP.getValue());
        paramObj.put("name", name);
        AutoexecCombopExecuteNodeConfigVo executeNodeConfig = new AutoexecCombopExecuteNodeConfigVo();
        executeNodeConfig.setSelectNodeList(selectNodeList);
        AutoexecCombopExecuteConfigVo executeConfig = new AutoexecCombopExecuteConfigVo();
        executeConfig.setExecuteNodeConfig(executeNodeConfig);
        paramObj.put("executeConfig", executeConfig);
        UserVo fcuVo = userMapper.getUserByUuid(userUuid);
        UserContext.init(fcuVo, SystemUser.SYSTEM.getTimezone());
        UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(fcuVo).getCc());
        IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
        AutoexecJobVo jobVo = JSONObject.toJavaObject(paramObj, AutoexecJobVo.class);
        autoexecJobActionCrossoverService.validateAndCreateJobFromCombop(jobVo);
        jobVo.setAction(JobAction.FIRE.getValue());
        IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
        JSONObject resultObj = fireAction.doService(jobVo);
        System.out.println("jobId=" + resultObj.get("jobId"));
    }
}

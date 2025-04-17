/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.inspect.api.job;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.crossover.IAutoexecJobActionCrossoverService;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteNodeConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.batch.BatchRunner;
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AppModuleVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.IResourceCenterDataSource;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.ResourceCenterDataSourceFactory;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.constvalue.JobSource;
import neatlogic.framework.inspect.dao.mapper.InspectMapper;
import neatlogic.framework.inspect.dto.InspectCiCombopVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Transactional
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class CreateInspectAppJobApi extends PrivateApiComponentBase {

    private static final Logger logger = LoggerFactory.getLogger(CreateInspectAppJobApi.class);
    @Resource
    AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    InspectMapper inspectMapper;

    @Override
    public String getName() {
        return "创建应用巡检作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "组合工具ID"),
            @Param(name = "envList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "环境列表"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "巡检状态列表")
    })
    @Output({})
    @Description(desc = "创建应用巡检作业")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystemVo = resourceCrossoverMapper.getAppSystemById(appSystemId);
        if (appSystemVo == null) {
            throw new AppSystemNotFoundException(appSystemId);
        }
        List<String> inspectStatusList = new ArrayList<>();
        JSONArray inspectStatusArray = paramObj.getJSONArray("inspectStatusList");
        if (CollectionUtils.isNotEmpty(inspectStatusArray)) {
            inspectStatusList = inspectStatusArray.toJavaList(String.class);
        }
//        inspectStatusList.add("warn");
//        inspectStatusList.add("critical");
//        inspectStatusList.add("fatal");
        Set<Long> allResourceTypeIdSet = new HashSet<>();
        IResourceCenterDataSource resourceCenterDataSource = ResourceCenterDataSourceFactory.getResourceCenterDataSource();
        List<ResourceSearchVo> searchList = new ArrayList<>();
        JSONArray envList = paramObj.getJSONArray("envList");
        for (int i = 0; i < envList.size(); i++) {
            JSONObject envObj = envList.getJSONObject(i);
            if (MapUtils.isEmpty(envObj)) {
                continue;
            }
            Long envId = envObj.getLong("envId");
            if (envId == null) {
                continue;
            }
            JSONArray appModuleIdArray = envObj.getJSONArray("appModuleIdList");
            if (CollectionUtils.isEmpty(appModuleIdArray)) {
                continue;
            }
            List<Long> appModuleIdList = appModuleIdArray.toJavaList(Long.class);
            for (Long appModuleId : appModuleIdList) {
                if (appModuleId == null) {
                    continue;
                }
                Set<Long> typeIdSet = new HashSet<>();
                Map<String, List<Long>> viewName2TypeIdListMap = resourceCenterDataSource.getAppResourceTypeIdListByAppSystemIdAndAppModuleIdAndEnvIdAndInspectStatusList(appSystemId, appModuleId, envId, inspectStatusList);
                for (Map.Entry<String, List<Long>> entry : viewName2TypeIdListMap.entrySet()) {
                    typeIdSet.addAll(entry.getValue());
                    allResourceTypeIdSet.addAll(entry.getValue());
                }
                ResourceSearchVo searchVo = new ResourceSearchVo();
                searchVo.setAppSystemId(appSystemId);
                searchVo.setAppModuleId(appModuleId);
                searchVo.setEnvId(envId);
                searchVo.setTypeIdList(new ArrayList<>(typeIdSet));
                searchList.add(searchVo);
            }
        }
        JSONArray resultList = new JSONArray();
        Map<Long, AppModuleVo> appModuleMap = new HashMap<>();
        Map<Long, String> appEnvId2NameMap = new HashMap<>();
        List<AutoexecJobVo> autoexecJobList = new ArrayList<>();
        Map<Long, CiVo> ciMap = new HashMap<>();
        Map<Long, Long> ciIdToCombopIdMap = new HashMap<>();
        Map<Long, AutoexecCombopVo> autoexecCombopMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(allResourceTypeIdSet)) {
            ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
            List<CiVo> ciVoList = ciCrossoverMapper.getCiByIdList(new ArrayList<>(allResourceTypeIdSet));
            ciMap = ciVoList.stream().filter(Objects::nonNull).collect(Collectors.toMap(CiVo::getId, e -> e));
            List<InspectCiCombopVo> ciCombopList = inspectMapper.searchInspectCiCombopListByCiIdList(new ArrayList<>(allResourceTypeIdSet));
            ciIdToCombopIdMap = ciCombopList.stream().filter(Objects::nonNull).collect(Collectors.toMap(InspectCiCombopVo::getId, InspectCiCombopVo::getCombopId));
            List<Long> combopIdList = ciCombopList.stream().filter(Objects::nonNull).map(InspectCiCombopVo::getCombopId).filter(Objects::nonNull).collect(Collectors.toList());
            List<AutoexecCombopVo> autoexecCombopList = autoexecCombopMapper.getAutoexecCombopByIdList(combopIdList);
            autoexecCombopMap = autoexecCombopList.stream().filter(Objects::nonNull).collect(Collectors.toMap(AutoexecCombopVo::getId, e -> e));
            List<AppModuleVo> appModuleList = resourceCenterDataSource.getAppModuleListForTree(appSystemId);
            appModuleMap = appModuleList.stream().filter(Objects::nonNull).collect(Collectors.toMap(AppModuleVo::getId, e -> e));
            BasePageVo basePageVo = new BasePageVo();
            List<ResourceVo> appEnvList = resourceCenterDataSource.getAppEnvListForSelect(basePageVo, false);
            appEnvId2NameMap = appEnvList.stream().filter(Objects::nonNull).collect(Collectors.toMap(ResourceVo::getId, ResourceVo::getName));
            appEnvId2NameMap.put(-2L, "未配置环境");
        }
        for (ResourceSearchVo searchVo : searchList) {
            AppModuleVo appModuleVo = appModuleMap.get(searchVo.getAppModuleId());
            String envName = appEnvId2NameMap.get(searchVo.getEnvId());
            if (CollectionUtils.isNotEmpty(searchVo.getTypeIdList())) {
                for (Long typeId : searchVo.getTypeIdList()) {
                    JSONObject jsonObj = new JSONObject(new LinkedHashMap<>());
                    jsonObj.put("appSystemId", searchVo.getAppSystemId());
                    jsonObj.put("appSystemName", appSystemVo.getName());
                    jsonObj.put("appSystemAbbrName", appSystemVo.getAbbrName());
                    jsonObj.put("appModuleId", searchVo.getAppModuleId());
                    if (appModuleVo != null) {
                        jsonObj.put("appModuleName", appModuleVo.getName());
                        jsonObj.put("appModuleAbbrName", appModuleVo.getAbbrName());
                    }
                    jsonObj.put("envId", searchVo.getEnvId());
                    jsonObj.put("envName", envName);
                    jsonObj.put("typeId", typeId);
                    CiVo ciVo = ciMap.get(typeId);
                    if (ciVo != null) {
                        jsonObj.put("typeName", ciVo.getName());
                        jsonObj.put("typeLabel", ciVo.getLabel());
                        Long combopId = ciIdToCombopIdMap.get(typeId);
                        if (combopId != null) {
                            jsonObj.put("combopId", combopId);
                            AutoexecCombopVo autoexecCombopVo = autoexecCombopMap.get(combopId);
                            if (autoexecCombopVo != null) {
                                jsonObj.put("combopName", autoexecCombopVo.getName());
                                AutoexecJobVo jobVo = new AutoexecJobVo();
                                jobVo.setRoundCount(64);
                                jobVo.setOperationId(combopId);
                                jobVo.setOperationType(CombopOperationType.COMBOP.getValue());
                                jobVo.setSource(JobSource.INSPECT_APP.getValue());
                                jobVo.setParam(new JSONObject());
                                jobVo.setName(ciVo.getLabel() + "(" + ciVo.getName() + ")");
                                jobVo.setInvokeId(typeId);
                                jobVo.setRouteId(appSystemId.toString());
                                AutoexecCombopExecuteConfigVo executeConfig = new AutoexecCombopExecuteConfigVo();
                                AutoexecCombopExecuteNodeConfigVo executeNodeConfig = new AutoexecCombopExecuteNodeConfigVo();
                                JSONObject filter = new JSONObject();
                                filter.put("typeId", typeId);
                                filter.put("envId", searchVo.getEnvId());
                                filter.put("appModuleId", searchVo.getAppModuleId());
                                filter.put("appSystemId", searchVo.getAppSystemId());
                                filter.put("inspectStatusList", inspectStatusList);
                                List<AutoexecNodeVo> autoexecNodeList = getAutoexecNodeList(filter);
                                jsonObj.put("执行目标列表", autoexecNodeList);
                                executeNodeConfig.setOtherFilter(filter);
                                executeConfig.setExecuteNodeConfig(executeNodeConfig);
                                jobVo.setExecuteConfig(executeConfig);
                                autoexecJobList.add(jobVo);
                                jsonObj.put("jobId", jobVo.getId());
                                jsonObj.put("jobName", jobVo.getName());
                            } else {
                                jsonObj.put("message", "combopId找不到对应的组合工具，请重新设置组合工具，不发起作业");
                            }
                        } else {
                            jsonObj.put("message", "对应的模型设置组合工具，不发起作业");
                        }
                    } else {
                        jsonObj.put("message", "typeId找不到对应的模型，不发起作业");
                    }
                    resultList.add(jsonObj);
                }
            } else {
                JSONObject jsonObj = new JSONObject(new LinkedHashMap<>());
                jsonObj.put("appSystemId", searchVo.getAppSystemId());
                jsonObj.put("appSystemName", appSystemVo.getName());
                jsonObj.put("appSystemAbbrName", appSystemVo.getAbbrName());
                jsonObj.put("appModuleId", searchVo.getAppModuleId());
                if (appModuleVo != null) {
                    jsonObj.put("appModuleName", appModuleVo.getName());
                    jsonObj.put("appModuleAbbrName", appModuleVo.getAbbrName());
                }
                jsonObj.put("envId", searchVo.getEnvId());
                jsonObj.put("envName", envName);
                jsonObj.put("message", "typeIdList为空，找不到数据，不发起作业");
                resultList.add(jsonObj);
            }
        }
        JSONObject resultObj = new JSONObject();
        if (CollectionUtils.isNotEmpty(autoexecJobList)) {
            ConcurrentMap<Long, Object> concurrentMap = new ConcurrentHashMap<>();
            BatchRunner<AutoexecJobVo> runner = new BatchRunner<>();
            runner.execute(autoexecJobList, 1, (threadIndex, dataIndex, jobVo) -> {
                try {
                    IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
                    autoexecJobActionCrossoverService.validateAndCreateJobFromCombop(jobVo);
                    IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
                    jobVo.setAction(JobAction.FIRE.getValue());
                    fireAction.doService(jobVo);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    concurrentMap.put(jobVo.getId(), ExceptionUtils.getStackFrames(e));
                }
            }, "INSPECT-APP-JOB-MULTI-CREATE");
            if (MapUtils.isNotEmpty(concurrentMap)) {
                for (Map.Entry<Long, Object> entry : concurrentMap.entrySet()) {
                    for (int i = 0; i < resultList.size(); i++) {
                        JSONObject jsonObj = resultList.getJSONObject(i);
                        Long jobId = jsonObj.getLong("jobId");
                        if (Objects.equals(jobId, entry.getKey())) {
                            jsonObj.put("message", "创建作业失败");
                            jsonObj.put("stackTrace", entry.getValue());
                            break;
                        }
                    }
                }
            }
        }
        resultObj.put("tbodyList", resultList);
        return resultObj;
    }

    @Override
    public String getToken() {
        return "inspect/app/job/create";
    }

    private List<AutoexecNodeVo> getAutoexecNodeList(JSONObject otherFilter) {
        List<AutoexecNodeVo> resultList = new ArrayList<>();
        Long appSystemId = otherFilter.getLong("appSystemId");
        Long appModuleId = otherFilter.getLong("appModuleId");
        Long envId = otherFilter.getLong("envId");
        Long typeId = otherFilter.getLong("typeId");
        JSONArray inspectStatusList = otherFilter.getJSONArray("inspectStatusList");
        ResourceSearchVo searchVo = new ResourceSearchVo();
        searchVo.setAppSystemId(appSystemId);
        searchVo.setAppModuleId(appModuleId);
        searchVo.setEnvId(envId);
        searchVo.setTypeId(typeId);
        searchVo.setInspectStatusList(inspectStatusList.toJavaList(String.class));
        IResourceCenterDataSource resourceCenterDataSource = ResourceCenterDataSourceFactory.getResourceCenterDataSource();
        List<ResourceEntityVo> appViewList = resourceCenterDataSource.getAppViewList();
        if (CollectionUtils.isNotEmpty(appViewList)) {
            for (ResourceEntityVo resourceEntityVo : appViewList) {
                searchVo.setViewName(resourceEntityVo.getName());
                searchVo.setCurrentPage(1);
                searchVo.setPageSize(100);
                List<ResourceVo> resourceList = resourceCenterDataSource.getAppResourceList(searchVo, false);
                for (ResourceVo resourceVo : resourceList) {
                    resultList.add(new AutoexecNodeVo(resourceVo));
                }
            }
        }
        return resultList;
    }

}

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
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AppModuleVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
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
import org.apache.commons.lang3.StringUtils;
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
            @Param(name = "viewName", type = ApiParamType.STRING, desc = "操作系统入口视图名"),
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
        String viewName = StringUtils.trimToNull(paramObj.getString("viewName"));
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
                if (viewName != null) {
                    List<Long> typeIdList = viewName2TypeIdListMap.getOrDefault(viewName, Collections.emptyList());
                    typeIdSet.addAll(typeIdList);
                    allResourceTypeIdSet.addAll(typeIdList);
                } else {
                    for (Map.Entry<String, List<Long>> entry : viewName2TypeIdListMap.entrySet()) {
                        typeIdSet.addAll(entry.getValue());
                        allResourceTypeIdSet.addAll(entry.getValue());
                    }
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
            ciIdToCombopIdMap = ciCombopList.stream().filter(Objects::nonNull).filter(e -> e.getCombopId() != null && e.getId() != null).collect(Collectors.toMap(InspectCiCombopVo::getId, InspectCiCombopVo::getCombopId));
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
                    jsonObj.put("isCreateJobSuccess", 0);
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
                                List<AutoexecNodeVo> autoexecNodeList = getAutoexecNodeList(filter, viewName);
                                jsonObj.put("执行目标列表", autoexecNodeList);
                                executeNodeConfig.setOtherFilter(filter);
                                executeConfig.setExecuteNodeConfig(executeNodeConfig);
                                jobVo.setExecuteConfig(executeConfig);
                                autoexecJobList.add(jobVo);
                                jsonObj.put("jobId", jobVo.getId());
                                jsonObj.put("jobName", jobVo.getName());
                            } else {
                                jsonObj.put("message", "在巡检定义中设置的组合工具已失效，请重新设置组合工具");
                            }
                        } else {
                            jsonObj.put("message", "在巡检定义中没设置组合工具");
                        }
                    } else {
                        jsonObj.put("message", "找不到模型："+typeId);
                    }
                    resultList.add(jsonObj);
                }
            } else {
                JSONObject jsonObj = new JSONObject(new LinkedHashMap<>());
                jsonObj.put("isCreateJobSuccess", 0);
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
            ConcurrentMap<Long, JSONObject> concurrentMap = new ConcurrentHashMap<>();
            for (AutoexecJobVo jobVo : autoexecJobList) {
                try {
                    IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
                    autoexecJobActionCrossoverService.validateAndCreateJobFromCombop(jobVo);
                    IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
                    jobVo.setAction(JobAction.FIRE.getValue());
                    fireAction.doService(jobVo);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    JSONObject errorObj = new JSONObject();
                    errorObj.put("errorMsg", StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getName()));
                    errorObj.put("stackTrace", ExceptionUtils.getStackFrames(e));
                    concurrentMap.put(jobVo.getId(), errorObj);
                }
            }
            for (int i = 0; i < resultList.size(); i++) {
                JSONObject jsonObj = resultList.getJSONObject(i);
                Long jobId = jsonObj.getLong("jobId");
                if (jobId == null) {
                    continue;
                }
                JSONObject errorObj = concurrentMap.get(jobId);
                if (errorObj == null) {
                    jsonObj.put("isCreateJobSuccess", 1);
                } else {
                    jsonObj.put("isCreateJobSuccess", 0);
                    jsonObj.put("message", "创建作业失败");
                    jsonObj.put("errorMsg", errorObj.getString("errorMsg"));
                    jsonObj.put("stackTrace", errorObj.get("stackTrace"));
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

    private List<AutoexecNodeVo> getAutoexecNodeList(JSONObject otherFilter, String viewName) {
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
//        List<ResourceEntityVo> appViewList = resourceCenterDataSource.getAppViewList();
        Map<String, List<String>> viewName2FieldListMap = resourceCenterDataSource.getApplicationListDisplayViewName2FieldListMap();
        if (MapUtils.isNotEmpty(viewName2FieldListMap)) {
            Set<Long> resourceIdSet = new HashSet<>();
            for (Map.Entry<String, List<String>> entry : viewName2FieldListMap.entrySet()) {
                if (StringUtils.isNotBlank(viewName) && !StringUtils.equals(viewName, entry.getKey())) {
                    continue;
                }
                searchVo.setViewName(entry.getKey());
                searchVo.setCurrentPage(1);
                searchVo.setPageSize(100);
                List<ResourceVo> resourceList = resourceCenterDataSource.getAppResourceList(searchVo, false);
                for (ResourceVo resourceVo : resourceList) {
                    if (!resourceIdSet.contains(resourceVo.getId())) {
                        resourceIdSet.add(resourceVo.getId());
                        resultList.add(new AutoexecNodeVo(resourceVo));
                    }
                }
            }
        }
        return resultList;
    }

}

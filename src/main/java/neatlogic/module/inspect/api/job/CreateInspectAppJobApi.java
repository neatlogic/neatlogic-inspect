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
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.crossover.IAutoexecJobActionCrossoverService;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
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
import neatlogic.framework.util.$;
import neatlogic.module.inspect.job.source.InspectAppJobRouteKey;
import neatlogic.module.inspect.service.InspectAppJobService;
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
    private AutoexecJobMapper autoexecJobMapper;
    @Resource
    InspectMapper inspectMapper;
    @Resource
    private InspectAppJobService inspectAppJobService;

    @Override
    public String getName() {
        return "nmiaj.createinspectappjobapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.inspect.appsystemid"),
            @Param(name = "envList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "term.inspect.envlist"),
            @Param(name = "viewName", type = ApiParamType.STRING, desc = "term.inspect.viewname"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "term.inspect.inspectstatuslist")
    })
    @Output({
            @Param(name = "parentJobId", type = ApiParamType.LONG, desc = "term.inspect.parentjobid"),
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, desc = "term.inspect.jobcreateresultlist")
    })
    @Description(desc = "nmiaj.createinspectappjobapi.getname")
    @ResubmitInterval(value = 2)
    /**
     * 按所选环境和模块创建父作业、子作业及报告范围快照。
     */
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
        Map<Long, String> typeIdToViewNameMap = new LinkedHashMap<>();
        IResourceCenterDataSource resourceCenterDataSource = ResourceCenterDataSourceFactory.getResourceCenterDataSource();
        Map<String, String> viewNameToLabelMap = new HashMap<>();
        JSONArray viewTableList = resourceCenterDataSource.getAppResourceList(appSystemId, null, null, null, null, 1, 1);
        for (int i = 0; i < viewTableList.size(); i++) {
            JSONObject table = viewTableList.getJSONObject(i);
            viewNameToLabelMap.put(table.getString("viewName"), table.getString("viewLabel"));
        }
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
                Set<Long> typeIdSet = new LinkedHashSet<>();
                Map<String, List<Long>> viewName2TypeIdListMap = resourceCenterDataSource.getAppResourceTypeIdListByAppSystemIdAndAppModuleIdAndEnvIdAndInspectStatusList(appSystemId, appModuleId, envId, inspectStatusList);
                if (viewName != null) {
                    List<Long> typeIdList = viewName2TypeIdListMap.getOrDefault(viewName, Collections.emptyList());
                    typeIdSet.addAll(typeIdList);
                    allResourceTypeIdSet.addAll(typeIdList);
                    typeIdList.forEach(typeId -> typeIdToViewNameMap.putIfAbsent(typeId, viewName));
                } else {
                    for (Map.Entry<String, List<Long>> entry : viewName2TypeIdListMap.entrySet()) {
                        typeIdSet.addAll(entry.getValue());
                        allResourceTypeIdSet.addAll(entry.getValue());
                        entry.getValue().forEach(typeId -> typeIdToViewNameMap.putIfAbsent(typeId, entry.getKey()));
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
        AutoexecJobVo parentJob = inspectAppJobService.createParentJob(appSystemId, appSystemVo.getName(), JobSource.INSPECT_APP, appSystemId);
        JSONArray snapshotAssetList = new JSONArray();
        // 手动选择范围可能包含多个环境和模块，统一按模型归集执行目标和报告资产。
        Map<Long, LinkedHashMap<Long, AutoexecNodeVo>> typeIdToNodeMap = new LinkedHashMap<>();
        Map<Long, List<JSONObject>> typeIdToSnapshotAssetListMap = new HashMap<>();
        Map<Long, Set<Long>> typeIdToSnapshotResourceIdSetMap = new HashMap<>();
        Map<Long, JSONObject> typeIdToResultMap = new LinkedHashMap<>();
        Map<Long, AutoexecJobVo> typeIdToJobMap = new LinkedHashMap<>();
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
            appEnvId2NameMap.put(-2L, $.t("term.inspect.unconfiguredenvironment"));
        }
        for (ResourceSearchVo searchVo : searchList) {
            AppModuleVo appModuleVo = appModuleMap.get(searchVo.getAppModuleId());
            String envName = appEnvId2NameMap.get(searchVo.getEnvId());
            if (CollectionUtils.isNotEmpty(searchVo.getTypeIdList())) {
                for (Long typeId : searchVo.getTypeIdList()) {
                    JSONObject filter = new JSONObject();
                    filter.put("typeId", typeId);
                    filter.put("envId", searchVo.getEnvId());
                    filter.put("appModuleId", searchVo.getAppModuleId());
                    filter.put("appSystemId", searchVo.getAppSystemId());
                    filter.put("inspectStatusList", inspectStatusList);
                    List<AutoexecNodeVo> autoexecNodeList = getAutoexecNodeList(filter, viewName);
                    LinkedHashMap<Long, AutoexecNodeVo> nodeMap = typeIdToNodeMap.computeIfAbsent(typeId, key -> new LinkedHashMap<>());
                    for (AutoexecNodeVo nodeVo : autoexecNodeList) {
                        nodeMap.putIfAbsent(nodeVo.getId(), nodeVo);
                    }
                    for (AutoexecNodeVo nodeVo : autoexecNodeList) {
                        Set<Long> snapshotResourceIdSet = typeIdToSnapshotResourceIdSetMap.computeIfAbsent(typeId, key -> new HashSet<>());
                        if (!snapshotResourceIdSet.add(nodeVo.getId())) {
                            continue;
                        }
                        JSONObject asset = new JSONObject(new LinkedHashMap<>());
                        String categoryName = typeIdToViewNameMap.get(typeId);
                        asset.put("category", StringUtils.defaultIfBlank(viewNameToLabelMap.get(categoryName), categoryName));
                        asset.put("resourceId", nodeVo.getId());
                        asset.put("name", nodeVo.getName());
                        asset.put("ip", nodeVo.getIp());
                        asset.put("port", nodeVo.getPort());
                        asset.put("typeId", typeId);
                        asset.put("typeLabel", nodeVo.getTypeLabel());
                        asset.put("appModuleId", searchVo.getAppModuleId());
                        asset.put("appModuleName", appModuleVo == null ? null : appModuleVo.getName());
                        asset.put("envId", searchVo.getEnvId());
                        asset.put("envName", envName);
                        snapshotAssetList.add(asset);
                        typeIdToSnapshotAssetListMap.computeIfAbsent(typeId, key -> new ArrayList<>()).add(asset);
                    }
                    typeIdToResultMap.computeIfAbsent(typeId, key -> {
                        JSONObject result = new JSONObject(new LinkedHashMap<>());
                        result.put("isCreateJobSuccess", 0);
                        result.put("appSystemId", searchVo.getAppSystemId());
                        result.put("appSystemName", appSystemVo.getName());
                        result.put("appSystemAbbrName", appSystemVo.getAbbrName());
                        result.put("typeId", typeId);
                        return result;
                    });
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
                jsonObj.put("message", $.t("nmiaj.createinspectappjobapi.resourcenotfound"));
                resultList.add(jsonObj);
            }
        }
        for (Map.Entry<Long, JSONObject> entry : typeIdToResultMap.entrySet()) {
            Long typeId = entry.getKey();
            JSONObject jsonObj = entry.getValue();
            CiVo ciVo = ciMap.get(typeId);
            if (ciVo == null) {
                jsonObj.put("message", $.t("nmiaj.createinspectappjobapi.cinotfound", typeId));
            } else {
                jsonObj.put("typeName", ciVo.getName());
                jsonObj.put("typeLabel", ciVo.getLabel());
                Long combopId = ciIdToCombopIdMap.get(typeId);
                if (combopId == null) {
                    jsonObj.put("message", $.t("nmiaj.createinspectappjobapi.combopnotconfigured"));
                } else {
                    jsonObj.put("combopId", combopId);
                    AutoexecCombopVo autoexecCombopVo = autoexecCombopMap.get(combopId);
                    if (autoexecCombopVo == null) {
                        jsonObj.put("message", $.t("nmiaj.createinspectappjobapi.combopinvalid"));
                    } else if (MapUtils.isEmpty(typeIdToNodeMap.get(typeId))) {
                        jsonObj.put("message", $.t("nmiaj.createinspectappjobapi.resourcenotfound"));
                    } else {
                        jsonObj.put("combopName", autoexecCombopVo.getName());
                        AutoexecJobVo jobVo = new AutoexecJobVo();
                        jobVo.setOperationId(combopId);
                        jobVo.setOperationType(CombopOperationType.COMBOP.getValue());
                        jobVo.setSource(JobSource.INSPECT_APP.getValue());
                        jobVo.setParentId(parentJob.getId());
                        jobVo.setParam(new JSONObject());
                        jobVo.setName(ciVo.getLabel() + "(" + ciVo.getName() + ")");
                        jobVo.setInvokeId(typeId);
                        jobVo.setRouteId(InspectAppJobRouteKey.ci(typeId));
                        AutoexecCombopExecuteNodeConfigVo executeNodeConfig = new AutoexecCombopExecuteNodeConfigVo();
                        executeNodeConfig.setSelectNodeList(new ArrayList<>(typeIdToNodeMap.get(typeId).values()));
                        AutoexecCombopExecuteConfigVo executeConfig = new AutoexecCombopExecuteConfigVo();
                        executeConfig.setExecuteNodeConfig(executeNodeConfig);
                        jobVo.setExecuteConfig(executeConfig);
                        autoexecJobList.add(jobVo);
                        typeIdToJobMap.put(typeId, jobVo);
                        jsonObj.put("jobName", jobVo.getName());
                    }
                }
            }
            List<JSONObject> assetList = typeIdToSnapshotAssetListMap.get(typeId);
            if (CollectionUtils.isNotEmpty(assetList)) {
                for (JSONObject asset : assetList) {
                    asset.put("jobId", jsonObj.getLong("jobId"));
                    asset.put("uninspectedReason", jsonObj.getString("message"));
                }
            }
            resultList.add(jsonObj);
        }
        JSONObject resultObj = new JSONObject();
        for (Map.Entry<Long, AutoexecJobVo> entry : typeIdToJobMap.entrySet()) {
            Long typeId = entry.getKey();
            AutoexecJobVo jobVo = entry.getValue();
            JSONObject jsonObj = typeIdToResultMap.get(typeId);
            List<JSONObject> assetList = typeIdToSnapshotAssetListMap.get(typeId);
            try {
                IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
                autoexecJobActionCrossoverService.validateAndCreateJobFromCombop(jobVo);
                jsonObj.put("jobId", jobVo.getId());
                if (CollectionUtils.isNotEmpty(assetList)) {
                    for (JSONObject asset : assetList) {
                        asset.put("jobId", jobVo.getId());
                    }
                }
                IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
                jobVo.setAction(JobAction.FIRE.getValue());
                fireAction.doService(jobVo);
                jsonObj.put("isCreateJobSuccess", 1);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                String errorMessage = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getName());
                jsonObj.put("isCreateJobSuccess", 0);
                jsonObj.put("message", $.t("nmiaj.createinspectappjobapi.createfailed"));
                jsonObj.put("errorMsg", errorMessage);
                jsonObj.put("stackTrace", ExceptionUtils.getStackFrames(e));
                if (CollectionUtils.isNotEmpty(assetList)) {
                    for (JSONObject asset : assetList) {
                        asset.put("jobId", jobVo.getId());
                        asset.put("uninspectedReason", $.t("nmiaj.createinspectappjobapi.createfaileddetail", errorMessage));
                    }
                }
            }
        }
        JSONObject snapshot = new JSONObject(new LinkedHashMap<>());
        snapshot.put("parentJobId", parentJob.getId());
        snapshot.put("appSystemId", appSystemId);
        snapshot.put("appSystemName", appSystemVo.getName());
        snapshot.put("appSystemAbbrName", appSystemVo.getAbbrName());
        snapshot.put("source", JobSource.INSPECT_APP.getValue());
        JSONArray snapshotEnvList = new JSONArray();
        for (int i = 0; i < envList.size(); i++) {
            JSONObject env = envList.getJSONObject(i);
            JSONObject snapshotEnv = new JSONObject(new LinkedHashMap<>());
            snapshotEnv.put("envId", env.getLong("envId"));
            snapshotEnv.put("envName", appEnvId2NameMap.get(env.getLong("envId")));
            snapshotEnv.put("appModuleIdList", env.getJSONArray("appModuleIdList"));
            snapshotEnvList.add(snapshotEnv);
        }
        snapshot.put("envList", snapshotEnvList);
        snapshot.put("appModuleIdList", searchList.stream().map(ResourceSearchVo::getAppModuleId).filter(Objects::nonNull).distinct().collect(Collectors.toList()));
        snapshot.put("assetList", snapshotAssetList);
        inspectAppJobService.saveSnapshot(snapshot);
        if (CollectionUtils.isEmpty(autoexecJobMapper.getJobIdListByParentId(parentJob.getId()))) {
            autoexecJobMapper.updateJobStatus(new AutoexecJobVo(parentJob.getId(), JobStatus.FAILED.getValue()));
        }
        resultObj.put("parentJobId", parentJob.getId());
        resultObj.put("tbodyList", resultList);
        return resultObj;
    }

    @Override
    public String getToken() {
        return "inspect/app/job/create";
    }

    /**
     * 获取指定应用范围和资源视图内的全部执行目标。
     */
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

/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.batch.BatchRunner;
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.constvalue.JobSource;
import neatlogic.framework.inspect.dao.mapper.InspectMapper;
import neatlogic.framework.inspect.exception.InspectCiCombopNotBindException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Transactional
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class CreateInspectAppJobApi extends PrivateApiComponentBase {

    private final static Logger logger = LoggerFactory.getLogger(CreateInspectAppJobApi.class);
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
            @Param(name = "envList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "环境列表")
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
        List<ResourceSearchVo> searchList = new ArrayList<>();
        List<Long> allEnvIdList = new ArrayList<>();
        Set<Long> allAppModuleIdSet = new HashSet<>();
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
            allEnvIdList.add(envId);
            JSONArray appModuleIdArray = envObj.getJSONArray("appModuleIdList");
            if (CollectionUtils.isEmpty(appModuleIdArray)) {
                continue;
            }
            List<Long> appModuleIdList = appModuleIdArray.toJavaList(Long.class);
            for (Long appModuleId : appModuleIdList) {
                if (appModuleId == null) {
                    continue;
                }
                allAppModuleIdSet.add(appModuleId);
                ResourceSearchVo searchVo = new ResourceSearchVo();
                searchVo.setAppSystemId(appSystemId);
                searchVo.setAppModuleId(appModuleId);
                searchVo.setEnvId(envId);
                searchList.add(searchVo);
            }
        }
        if (CollectionUtils.isEmpty(searchList)) {
            return null;
        }
        List<AutoexecJobVo> autoexecJobList = new ArrayList<>();
        List<String> inspectStatusList = new ArrayList<>();
        Set<Long> allResourceTypeIdSet = new HashSet<>();
        inspectStatusList.add("warn");
        inspectStatusList.add("critical");
        inspectStatusList.add("fatal");
        for (ResourceSearchVo searchVo : searchList) {
            searchVo.setInspectStatusList(inspectStatusList);
            Set<Long> resourceTypeIdSet = resourceCrossoverMapper.getResourceTypeIdListByAppSystemIdAndModuleIdAndEnvIdAndInspectStatusList(searchVo);
            if (CollectionUtils.isEmpty(resourceTypeIdSet)) {
                continue;
            }
            allResourceTypeIdSet.addAll(resourceTypeIdSet);
            List<Long> resourceTypeIdList = new ArrayList<>(resourceTypeIdSet);
            searchVo.setTypeIdList(resourceTypeIdList);
        }
        Map<Long, CiVo> ciMap = new HashMap<>();
        Map<Long, Long> ciIdToCombopIdMap = new HashMap<>();
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        List<CiVo> ciVoList = ciCrossoverMapper.getCiByIdList(new ArrayList<>(allResourceTypeIdSet));
        for (CiVo ciVo : ciVoList) {
            Long ciId = ciVo.getId();
            Long combopId = inspectMapper.getCombopIdByCiId(ciId);
            if(combopId == null){
                continue;
            }
            AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
            if (combopVo == null) {
                continue;
            }
            ciMap.put(ciId, ciVo);
            ciIdToCombopIdMap.put(ciId, combopId);
        }
        for (ResourceSearchVo searchVo : searchList) {
            if (CollectionUtils.isEmpty(searchVo.getTypeIdList())) {
                continue;
            }
            for (Long ciId : searchVo.getTypeIdList()) {
                Long combopId = ciIdToCombopIdMap.get(ciId);
                if (combopId == null) {
                    continue;
                }
                CiVo ciVo = ciMap.get(ciId);
                AutoexecJobVo jobVo = new AutoexecJobVo();
                jobVo.setRoundCount(64);
                jobVo.setOperationId(combopId);
                jobVo.setOperationType(CombopOperationType.COMBOP.getValue());
                jobVo.setSource(JobSource.INSPECT_APP.getValue());
                jobVo.setParam(new JSONObject());
                jobVo.setName(ciVo.getLabel() + "(" + ciVo.getName() + ")");
                jobVo.setInvokeId(ciId);
                jobVo.setRouteId(appSystemId.toString());
                AutoexecCombopExecuteConfigVo executeConfig = new AutoexecCombopExecuteConfigVo();
                AutoexecCombopExecuteNodeConfigVo executeNodeConfig = new AutoexecCombopExecuteNodeConfigVo();
                JSONObject filter = paramObj.getJSONObject("filter");
                if (filter == null) {
                    filter = new JSONObject();
                }
                List<Long> typeIdList = new ArrayList<>();
                typeIdList.add(ciId);
                filter.put("typeIdList", typeIdList);
                List<Long> envIdList = new ArrayList<>();
                envIdList.add(searchVo.getEnvId());
                filter.put("envIdList", envIdList);
                List<Long> appModuleIdList = new ArrayList<>();
                appModuleIdList.add(searchVo.getAppModuleId());
                filter.put("appModuleIdList", appModuleIdList);
                List<Long> appSystemIdList = new ArrayList<>();
                appSystemIdList.add(searchVo.getAppSystemId());
                filter.put("appSystemIdList", appSystemIdList);
                executeNodeConfig.setFilter(filter);
                executeConfig.setExecuteNodeConfig(executeNodeConfig);
                jobVo.setExecuteConfig(executeConfig);
                autoexecJobList.add(jobVo);
            }
        }
        if (CollectionUtils.isEmpty(autoexecJobList)) {
            return null;
        }

        BatchRunner<AutoexecJobVo> runner = new BatchRunner<>();
        runner.execute(autoexecJobList, 3, jobVo -> {
            try {
                IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
                autoexecJobActionCrossoverService.validateAndCreateJobFromCombop(jobVo);
                IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
                jobVo.setAction(JobAction.FIRE.getValue());
                jobVo.setIsFirstFire(1);
                fireAction.doService(jobVo);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }, "INSPECT-APP-JOB-MULTI-CREATE");
        return null;
    }

    @Override
    public String getToken() {
        return "inspect/app/job/create";
    }
}

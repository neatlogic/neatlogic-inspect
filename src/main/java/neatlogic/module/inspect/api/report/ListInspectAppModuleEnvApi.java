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

package neatlogic.module.inspect.api.report;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AppEnvVo;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.IResourceCenterDataSource;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.ResourceCenterDataSourceFactory;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListInspectAppModuleEnvApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "获取发起模块巡检的环境列表";
    }

    @Override
    public String getToken() {
        return "inspect/appmodule/env/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appmoduleid"),
            @Param(name = "viewName", type = ApiParamType.STRING, desc = "操作系统入口视图名"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "巡检状态列表")
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.JSONARRAY, desc = "环境列表")
    })
    @Description(desc = "获取发起模块巡检的环境列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray returnArray = new JSONArray();
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");
        List<String> inspectStatusList = new ArrayList<>();
        JSONArray inspectStatusArray = paramObj.getJSONArray("inspectStatusList");
        if (CollectionUtils.isNotEmpty(inspectStatusArray)) {
            inspectStatusList = inspectStatusArray.toJavaList(String.class);
        }
        IResourceCenterDataSource resourceCenterDataSource = ResourceCenterDataSourceFactory.getResourceCenterDataSource();
        List<AppEnvVo> appEnvList = resourceCenterDataSource.getAppEnvListByAppSystemIdAndAppModuleIdAndInspectStatusList(appSystemId, appModuleId, inspectStatusList);
        String viewName = StringUtils.trimToNull(paramObj.getString("viewName"));
        Set<Long> targetTypeIdSet = null;
        if (viewName != null) {
            targetTypeIdSet = new HashSet<>(resourceCenterDataSource
                    .getAppResourceTypeIdListByAppSystemIdAndAppModuleIdAndEnvIdAndInspectStatusList(appSystemId, appModuleId, null, inspectStatusList)
                    .getOrDefault(viewName, new ArrayList<>()));
        }
        for (AppEnvVo appEnvVo : appEnvList) {
            if (appEnvVo == null || CollectionUtils.isEmpty(appEnvVo.getAppModuleList()) || appEnvVo.getAppModuleList().get(0) == null) {
                continue;
            }
            List<CiVo> ciVoList = appEnvVo.getAppModuleList().get(0).getCiList();
            if (targetTypeIdSet != null) {
                List<CiVo> filterCiVoList = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(ciVoList)) {
                    for (CiVo ciVo : ciVoList) {
                        if (ciVo != null && targetTypeIdSet.contains(ciVo.getId())) {
                            filterCiVoList.add(ciVo);
                        }
                    }
                }
                if (CollectionUtils.isEmpty(filterCiVoList)) {
                    continue;
                }
                ciVoList = filterCiVoList;
            }
            JSONObject returnObj = new JSONObject();
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("id", appEnvVo.getId());
            jsonObj.put("name", appEnvVo.getName());
            returnObj.put("env", jsonObj);
            returnObj.put("ciVoList", ciVoList);
            returnArray.add(returnObj);
        }
//        List<ResourceVo> envResourceList = new ArrayList<>();
//        BasePageVo search = new BasePageVo();
//        search.setCurrentPage(1);
//        search.setPageSize(100);
//        List<Long> envIdList = resourceMapper.searchAppEnvIdList(search);
//        if (CollectionUtils.isNotEmpty(envIdList)) {
//            envResourceList = resourceMapper.searchAppEnvListByIdList(envIdList);
//        }
//        //获取数据库所有的模型，用于通过id去获得对应的模型
//        Map<Long, CiVo> allCiVoMap = new HashMap<>();
//        List<CiVo> allCiVoList = ciMapper.getAllCi(null);
//        for (CiVo ci : allCiVoList) {
//            allCiVoMap.put(ci.getId(), ci);
//        }
//        //无配置环境
//        ResourceVo noSettingEnvResourceVo = new ResourceVo();
//        noSettingEnvResourceVo.setId(-2L);
//        noSettingEnvResourceVo.setName("未配置");
//        envResourceList.add(noSettingEnvResourceVo);
//        for (ResourceVo envResource : envResourceList) {
//            JSONObject returnObj = new JSONObject();
//            Set<Long> typeIdSet = new HashSet<>();
//            IResourceCenterDataSource resourceCenterDataSource = ResourceCenterDataSourceFactory.getResourceCenterDataSource();
//            Map<String, List<Long>> viewName2TypeIdListMap = resourceCenterDataSource.getAppResourceTypeIdListByAppSystemIdAndAppModuleIdAndEnvId(null, appModuleId, envResource.getId());
//            for (Map.Entry<String, List<Long>> entry : viewName2TypeIdListMap.entrySet()) {
//                typeIdSet.addAll(entry.getValue());
//            }
//            Set<CiVo> returnCiVoSet = new HashSet<>();
//            for (Long typeId : typeIdSet) {
//                CiVo ciVo = allCiVoMap.get(typeId);
//                if (ciVo == null) {
//                    throw new CiNotFoundException(typeId);
//                }
//                returnCiVoSet.add(ciVo);
//            }
//            if (CollectionUtils.isNotEmpty(returnCiVoSet)) {
//                returnObj.put("env", envResource);
//                returnObj.put("ciVoList", returnCiVoSet);
//                returnArray.add(returnObj);
//            }
//        }
        return returnArray;
    }
}

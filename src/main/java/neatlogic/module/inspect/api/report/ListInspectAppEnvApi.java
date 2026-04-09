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
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AppModuleVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AppEnvVo;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.IResourceCenterDataSource;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.ResourceCenterDataSourceFactory;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
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

/**
 * @author longrf
 * @date 2022/11/30 18:27
 */

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
@AuthAction(action = INSPECT_BASE.class)
public class ListInspectAppEnvApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "nmiar.listinspectappenvapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "inspect/app/env/list";
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "viewName", type = ApiParamType.STRING, desc = "操作系统入口视图名"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "巡检状态列表")
    })
    @Output({
            @Param(explode = AppEnvVo[].class, desc = "common.tbodylist")
    })
    @Description(desc = "nmiar.listinspectappenvapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        List<String> inspectStatusList = new ArrayList<>();
        JSONArray inspectStatusArray = paramObj.getJSONArray("inspectStatusList");
        if (CollectionUtils.isNotEmpty(inspectStatusArray)) {
            inspectStatusList = inspectStatusArray.toJavaList(String.class);
        }
//        inspectStatusList.add("warn");
//        inspectStatusList.add("critical");
//        inspectStatusList.add("fatal");
        IResourceCenterDataSource resourceCenterDataSource = ResourceCenterDataSourceFactory.getResourceCenterDataSource();
        List<AppEnvVo> appEnvList = resourceCenterDataSource.getAppEnvListByAppSystemIdAndAppModuleIdAndInspectStatusList(appSystemId, null, inspectStatusList);
        String viewName = StringUtils.trimToNull(paramObj.getString("viewName"));
        if (viewName == null) {
            return appEnvList;
        }
        Set<Long> targetTypeIdSet = new HashSet<>(resourceCenterDataSource
                .getAppResourceTypeIdListByAppSystemIdAndAppModuleIdAndEnvIdAndInspectStatusList(appSystemId, null, null, inspectStatusList)
                .getOrDefault(viewName, new ArrayList<>()));
        if (CollectionUtils.isEmpty(targetTypeIdSet)) {
            return new ArrayList<>();
        }
        List<AppEnvVo> resultList = new ArrayList<>();
        for (AppEnvVo appEnvVo : appEnvList) {
            if (appEnvVo == null || CollectionUtils.isEmpty(appEnvVo.getAppModuleList())) {
                continue;
            }
            List<AppModuleVo> appModuleList = new ArrayList<>();
            for (AppModuleVo appModuleVo : appEnvVo.getAppModuleList()) {
                if (appModuleVo == null || CollectionUtils.isEmpty(appModuleVo.getCiList())) {
                    continue;
                }
                List<CiVo> ciList = new ArrayList<>();
                for (CiVo ciVo : appModuleVo.getCiList()) {
                    if (ciVo != null && targetTypeIdSet.contains(ciVo.getId())) {
                        ciList.add(ciVo);
                    }
                }
                if (CollectionUtils.isNotEmpty(ciList)) {
                    AppModuleVo moduleVo = new AppModuleVo();
                    moduleVo.setId(appModuleVo.getId());
                    moduleVo.setName(appModuleVo.getName());
                    moduleVo.setAbbrName(appModuleVo.getAbbrName());
                    moduleVo.setCiList(ciList);
                    appModuleList.add(moduleVo);
                }
            }
            if (CollectionUtils.isNotEmpty(appModuleList)) {
                appEnvVo.setAppModuleList(appModuleList);
                resultList.add(appEnvVo);
            }
        }
        return resultList;
    }
}

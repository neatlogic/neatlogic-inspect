/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.inspect.api.report;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.resourcecenter.AppEnvVo;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.IResourceCenterDataSource;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.ResourceCenterDataSourceFactory;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/3/2 4:10 下午
 */
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
        for (AppEnvVo appEnvVo : appEnvList) {
            JSONObject returnObj = new JSONObject();
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("id", appEnvVo.getId());
            jsonObj.put("name", appEnvVo.getName());
            returnObj.put("env", jsonObj);
            returnObj.put("ciVoList", appEnvVo.getAppModuleList().get(0).getCiList());
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

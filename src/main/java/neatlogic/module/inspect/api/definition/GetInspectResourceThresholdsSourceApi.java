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
package neatlogic.module.inspect.api.definition;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.crossover.ISyncCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.enums.sync.CollectMode;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.service.InspectCollectService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author longrf
 * @date 2022/11/21 17:10
 */

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetInspectResourceThresholdsSourceApi extends PrivateApiComponentBase {

    @Resource
    private InspectCollectService inspectCollectService;

    @Override
    public String getName() {
        return "获取应用巡检阈值来源";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "inspect/resource/thresholds/source/get";
    }

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "资产id")
    })
    @Output({
            @Param(name = "resourceVo", explode = ResourceVo[].class, desc = "资产信息"),
            @Param(name = "appSystemVoList", explode = ResourceVo[].class, desc = "应用系统信息列表")
    })
    @Description(desc = "获取应用巡检阈值来源，需要依赖mongodb")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject returnObj = new JSONObject();
        Long resourceId = paramObj.getLong("resourceId");
        IResourceCenterResourceCrossoverService resourceCenterResourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
        ResourceVo resourceVo = resourceCenterResourceCrossoverService.getResourceById(resourceId);
        if (resourceVo == null) {
            throw new ResourceNotFoundException(resourceId);
        }
        ISyncCrossoverMapper iSyncCrossoverMapper = CrossoverServiceFactory.getApi(ISyncCrossoverMapper.class);
        String collectionName = iSyncCrossoverMapper.getSyncCiCollectionNameListByCiNameAndCollectMode(resourceVo.getTypeName(), CollectMode.INITIATIVE.getValue());
        returnObj.put("resourceVo", resourceVo);
        returnObj.put("collectionName", collectionName);
        List<Long> returnAppSystemIdList = inspectCollectService.getCollectionThresholdsAppSystemIdListByResourceId(resourceId);
        if (CollectionUtils.isNotEmpty(returnAppSystemIdList)) {
            IResourceCrossoverMapper iResourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            returnObj.put("appSystemVoList", iResourceCrossoverMapper.searchAppSystemListByIdList(returnAppSystemIdList));
        }
        return returnObj;
    }
}

/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

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

package neatlogic.module.inspect.api.report;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.inspect.dao.mapper.InspectMapper;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Service
public class InspectCiCombopGetApi extends PrivateApiComponentBase {
    @Resource
    AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    InspectMapper inspectMapper;

    @Override
    public String getName() {
        return "获取巡检Ci绑定的组合工具";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciId", type = ApiParamType.LONG, desc = "ci id"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资产id"),
    })

    @Description(desc = "根据CiId|资产id 获取巡检Ci绑定的组合工具")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject result = new JSONObject();
        AutoexecCombopVo combopVo = null;
        result.put("isHasBindCombop",1);
        Long ciId = paramObj.getLong("ciId");
        Long resourceId = paramObj.getLong("resourceId");
        if(resourceId != null){
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            List<ResourceVo> resourceVoList = resourceCrossoverMapper.getResourceByIdList(Collections.singletonList(resourceId));
            if(CollectionUtils.isEmpty(resourceVoList)){
                throw new ResourceNotFoundException(resourceId);
            }
            ciId = resourceVoList.get(0).getTypeId();
        }
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo ciVo = ciCrossoverMapper.getCiById(ciId);
        if(ciVo == null){
            throw new CiNotFoundException(ciId);
        }
        Long combopId = inspectMapper.getCombopIdByCiId(ciId);
        if(combopId != null){
            combopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
        }
        if (combopVo == null) {
            result.put("isHasBindCombop", 0);
        }
        result.put("combop",combopVo);
        return result;
    }

    @Override
    public String getToken() {
        return "inspect/ci/combop/get";
    }
}

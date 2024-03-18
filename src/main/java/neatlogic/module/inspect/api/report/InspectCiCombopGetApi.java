/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.inspect.api.report;

import com.alibaba.fastjson.JSONObject;
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

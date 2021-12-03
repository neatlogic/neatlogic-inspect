/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.cmdb.crossover.ICiCrossoverMapper;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dao.mapper.InspectMapper;
import codedriver.framework.inspect.exception.InspectCiCombopNotBindException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
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
    ResourceCenterMapper resourceCenterMapper;
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
        Long ciId = paramObj.getLong("ciId");
        Long resourceId = paramObj.getLong("resourceId");
        if(resourceId != null){
            List<ResourceVo> resourceVoList = resourceCenterMapper.getResourceByIdList(Collections.singletonList(resourceId), TenantContext.get().getDataDbName());
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
        if(combopId == null){
            throw  new InspectCiCombopNotBindException(ciVo.getLabel());
        }
        AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
        if(combopVo == null){
            throw new AutoexecCombopNotFoundException(combopId);
        }
        return combopVo;
    }

    @Override
    public String getToken() {
        return "inspect/ci/combop/get";
    }
}

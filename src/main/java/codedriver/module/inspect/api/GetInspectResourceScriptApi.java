package codedriver.module.inspect.api;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceScriptVo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.inspect.auth.INSPECT_MODIFY;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetInspectResourceScriptApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Override
    public String getName() {
        return "获取资源中心脚本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "inspect/resource/script/get";
    }

    @Input({
            @Param(name = "resourceId", isRequired = true, type = ApiParamType.LONG, desc = "资源id")
    })
    @Output({
            @Param(explode = ResourceScriptVo.class)
    })
    @Description(desc = "根据资源id获取对应的脚本")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long resourceId = paramObj.getLong("resourceId");
        String schemaName = TenantContext.get().getDataDbName();
        if (resourceCenterMapper.checkResourceIsExists(resourceId, schemaName) == 0) {
            throw new ResourceNotFoundException(resourceId);
        }
        return resourceCenterMapper.getResourceScriptByResourceId(resourceId);
    }
}

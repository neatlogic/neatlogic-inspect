package neatlogic.module.inspect.api.report;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.dao.mapper.InspectMapper;
import neatlogic.framework.inspect.dto.InspectResourceScriptVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_MODIFY;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetInspectAccessEndPointScriptApi extends PrivateApiComponentBase {

    @Resource
    private InspectMapper inspectMapper;

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
        return "inspect/accessendpoint/script/get";
    }

    @Input({
            @Param(name = "resourceId", isRequired = true, type = ApiParamType.LONG, desc = "资源id")
    })
    @Output({
            @Param(explode = InspectResourceScriptVo.class)
    })
    @Description(desc = "根据资源id获取对应的脚本")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long resourceId = paramObj.getLong("resourceId");
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        if (resourceCrossoverMapper.checkResourceIsExists(resourceId) == 0) {
            throw new ResourceNotFoundException(resourceId);
        }
        return inspectMapper.getResourceScriptByResourceId(resourceId);
    }
}

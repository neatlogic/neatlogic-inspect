package neatlogic.module.inspect.api.configcompare;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.service.InspectConfigCompareService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class CompareInspectConfigResourceApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigCompareService inspectConfigCompareService;

    @Override
    public String getName() {
        return "资源配置互相比对";
    }

    @Override
    public String getToken() {
        return "inspect/config/resource/compare";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "应用模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "资源模型id"),
            @Param(name = "sourceResourceId", type = ApiParamType.LONG, isRequired = true, desc = "源资源id"),
            @Param(name = "targetResourceId", type = ApiParamType.LONG, isRequired = true, desc = "目标资源id"),
            @Param(name = "schemaName", type = ApiParamType.STRING, isRequired = true, desc = "采集维度")
    })
    @Description(desc = "资源配置互相比对")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return inspectConfigCompareService.compareResource(
                paramObj.getLong("appSystemId"),
                paramObj.getLong("appModuleId"),
                paramObj.getLong("envId"),
                paramObj.getLong("typeId"),
                paramObj.getLong("sourceResourceId"),
                paramObj.getLong("targetResourceId"),
                paramObj.getString("schemaName")
        );
    }
}

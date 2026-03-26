package neatlogic.module.inspect.api.configcompare;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_MODIFY;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.service.InspectConfigCompareService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class SaveInspectConfigBaselineVersionApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigCompareService inspectConfigCompareService;

    @Override
    public String getName() {
        return "保存巡检配置基线草稿版本";
    }

    @Override
    public String getToken() {
        return "inspect/config/baseline/version/save";
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
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "资源id"),
            @Param(name = "schemaName", type = ApiParamType.STRING, isRequired = true, desc = "采集维度"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "基线名称"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "基线说明")
    })
    @Output({
            @Param(name = "version", type = ApiParamType.JSONOBJECT, desc = "基线版本")
    })
    @Description(desc = "从当前资源采集快照保存巡检配置基线草稿版本")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject result = new JSONObject();
        result.put("version", inspectConfigCompareService.promoteResourceToBaseline(
                paramObj.getLong("appSystemId"),
                paramObj.getLong("appModuleId"),
                paramObj.getLong("envId"),
                paramObj.getLong("typeId"),
                paramObj.getLong("resourceId"),
                paramObj.getString("schemaName"),
                paramObj.getString("name"),
                paramObj.getString("description")
        ));
        return result;
    }
}

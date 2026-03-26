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
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetInspectConfigAiSettingApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigCompareService inspectConfigCompareService;

    @Override
    public String getName() {
        return "获取巡检配置比对AI模型设置";
    }

    @Override
    public String getToken() {
        return "inspect/config/ai/setting/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "schemaName", type = ApiParamType.STRING, isRequired = true, desc = "采集维度")
    })
    @Output({
            @Param(name = "setting", type = ApiParamType.JSONOBJECT, desc = "模型设置"),
            @Param(name = "model", type = ApiParamType.JSONOBJECT, desc = "当前模型"),
            @Param(name = "modelList", type = ApiParamType.JSONARRAY, desc = "可选模型"),
            @Param(name = "defaultPrompt", type = ApiParamType.STRING, desc = "默认提示词"),
            @Param(name = "effectivePrompt", type = ApiParamType.STRING, desc = "生效提示词")
    })
    @Description(desc = "获取巡检配置比对AI模型设置")
    @Override
    public Object myDoService(JSONObject paramObj) {
        return inspectConfigCompareService.getAiSettingData(paramObj.getString("schemaName"));
    }
}

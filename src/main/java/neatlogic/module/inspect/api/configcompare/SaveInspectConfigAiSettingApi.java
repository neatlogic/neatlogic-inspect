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
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveInspectConfigAiSettingApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigCompareService inspectConfigCompareService;

    @Override
    public String getName() {
        return "保存巡检配置比对AI模型设置";
    }

    @Override
    public String getToken() {
        return "inspect/config/ai/setting/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "schemaName", type = ApiParamType.STRING, isRequired = true, desc = "采集维度"),
            @Param(name = "modelId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "prompt", type = ApiParamType.STRING, desc = "提示词")
    })
    @Output({
            @Param(name = "setting", type = ApiParamType.JSONOBJECT, desc = "模型设置")
    })
    @Description(desc = "保存巡检配置比对AI模型设置")
    @Override
    public Object myDoService(JSONObject paramObj) {
        JSONObject result = new JSONObject();
        result.put("setting", inspectConfigCompareService.saveAiSetting(paramObj.getString("schemaName"), paramObj.getLong("modelId"), paramObj.getString("prompt")));
        return result;
    }
}

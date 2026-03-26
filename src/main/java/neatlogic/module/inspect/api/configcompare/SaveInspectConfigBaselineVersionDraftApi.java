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
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveInspectConfigBaselineVersionDraftApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigCompareService inspectConfigCompareService;

    @Override
    public String getName() {
        return "保存巡检配置基线草稿";
    }

    @Override
    public String getToken() {
        return "inspect/config/baseline/version/draft/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "基线版本id"),
            @Param(name = "baselineData", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "草稿内容")
    })
    @Output({
            @Param(name = "version", type = ApiParamType.JSONOBJECT, desc = "基线版本")
    })
    @Description(desc = "保存巡检配置基线草稿")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject result = new JSONObject();
        Object baselineData = paramObj.get("baselineData");
        String baselineDataStr = baselineData instanceof String ? (String) baselineData : JSONObject.toJSONString(baselineData);
        result.put("version", inspectConfigCompareService.saveDraftBaselineVersion(paramObj.getLong("id"), baselineDataStr));
        return result;
    }
}

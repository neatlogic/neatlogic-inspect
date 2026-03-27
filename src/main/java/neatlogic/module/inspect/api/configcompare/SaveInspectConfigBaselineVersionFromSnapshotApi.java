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
public class SaveInspectConfigBaselineVersionFromSnapshotApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigCompareService inspectConfigCompareService;

    @Override
    public String getName() {
        return "从快照生成巡检配置基线草稿版本";
    }

    @Override
    public String getToken() {
        return "inspect/config/baseline/version/snapshot/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "snapshotId", type = ApiParamType.LONG, isRequired = true, desc = "快照id"),
            @Param(name = "baselineData", type = ApiParamType.JSONOBJECT, desc = "基线草稿内容"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "基线名称"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "基线说明")
    })
    @Output({
            @Param(name = "version", type = ApiParamType.JSONOBJECT, desc = "基线版本")
    })
    @Description(desc = "从指定快照生成巡检配置基线草稿版本")
    @Override
    public Object myDoService(JSONObject paramObj) {
        String baselineData = null;
        Object baselineDataObj = paramObj.get("baselineData");
        if (baselineDataObj instanceof JSONObject) {
            baselineData = ((JSONObject) baselineDataObj).toJSONString();
        } else if (baselineDataObj instanceof String) {
            baselineData = (String) baselineDataObj;
        }
        JSONObject result = new JSONObject();
        result.put("version", inspectConfigCompareService.promoteSnapshotToBaseline(
                paramObj.getLong("snapshotId"),
                baselineData,
                paramObj.getString("name"),
                paramObj.getString("description")
        ));
        return result;
    }
}

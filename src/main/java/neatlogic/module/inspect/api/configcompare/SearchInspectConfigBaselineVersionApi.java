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
public class SearchInspectConfigBaselineVersionApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigCompareService inspectConfigCompareService;

    @Override
    public String getName() {
        return "查询巡检配置基线版本列表";
    }

    @Override
    public String getToken() {
        return "inspect/config/baseline/version/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "baselineId", type = ApiParamType.LONG, isRequired = true, desc = "基线id")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, desc = "版本列表")
    })
    @Description(desc = "查询巡检配置基线版本列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject result = new JSONObject();
        result.put("tbodyList", inspectConfigCompareService.getBaselineVersionListByBaselineId(paramObj.getLong("baselineId")));
        return result;
    }
}

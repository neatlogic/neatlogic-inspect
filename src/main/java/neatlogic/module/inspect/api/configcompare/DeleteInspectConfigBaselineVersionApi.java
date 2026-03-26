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
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteInspectConfigBaselineVersionApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigCompareService inspectConfigCompareService;

    @Override
    public String getName() {
        return "删除巡检配置基线版本";
    }

    @Override
    public String getToken() {
        return "inspect/config/baseline/version/delete";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "基线版本id")
    })
    @Output({
            @Param(name = "baselineDeleted", type = ApiParamType.BOOLEAN, desc = "删除后是否已清空整条基线")
    })
    @Description(desc = "删除巡检配置基线版本")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject result = new JSONObject();
        result.put("baselineDeleted", inspectConfigCompareService.deleteBaselineVersion(paramObj.getLong("id")));
        return result;
    }
}

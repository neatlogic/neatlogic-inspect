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
public class RollbackInspectConfigBaselineVersionApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigCompareService inspectConfigCompareService;

    @Override
    public String getName() {
        return "回退巡检配置基线版本";
    }

    @Override
    public String getToken() {
        return "inspect/config/baseline/version/rollback";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "版本id")
    })
    @Output({
            @Param(name = "version", type = ApiParamType.JSONOBJECT, desc = "基线版本")
    })
    @Description(desc = "回退巡检配置基线版本")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject result = new JSONObject();
        result.put("version", inspectConfigCompareService.rollbackBaselineVersion(paramObj.getLong("id")));
        return result;
    }
}

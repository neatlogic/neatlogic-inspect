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
public class AuditInspectConfigBaselineVersionApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigCompareService inspectConfigCompareService;

    @Override
    public String getName() {
        return "审批巡检配置基线版本";
    }

    @Override
    public String getToken() {
        return "inspect/config/baseline/version/audit";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "版本id"),
            @Param(name = "stage", type = ApiParamType.STRING, desc = "兼容字段，固定为approval"),
            @Param(name = "status", type = ApiParamType.STRING, isRequired = true, desc = "审批结果approved/rejected"),
            @Param(name = "comment", type = ApiParamType.STRING, desc = "审批意见")
    })
    @Output({
            @Param(name = "version", type = ApiParamType.JSONOBJECT, desc = "基线版本")
    })
    @Description(desc = "审批巡检配置基线版本")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject result = new JSONObject();
        result.put("version", inspectConfigCompareService.auditBaselineVersion(
                paramObj.getLong("id"),
                paramObj.getString("stage"),
                paramObj.getString("status"),
                paramObj.getString("comment")
        ));
        return result;
    }
}

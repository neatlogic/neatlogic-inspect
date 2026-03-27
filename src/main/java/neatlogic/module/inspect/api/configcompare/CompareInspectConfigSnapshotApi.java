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
public class CompareInspectConfigSnapshotApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigCompareService inspectConfigCompareService;

    @Override
    public String getName() {
        return "快照与当前配置基线比对";
    }

    @Override
    public String getToken() {
        return "inspect/config/snapshot/compare";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "snapshotId", type = ApiParamType.LONG, isRequired = true, desc = "快照id")
    })
    @Description(desc = "快照与当前配置基线比对")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return inspectConfigCompareService.compareSnapshotWithBaseline(paramObj.getLong("snapshotId"));
    }
}

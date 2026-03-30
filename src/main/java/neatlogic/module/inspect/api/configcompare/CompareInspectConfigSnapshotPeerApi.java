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
public class CompareInspectConfigSnapshotPeerApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigCompareService inspectConfigCompareService;

    @Override
    public String getName() {
        return "快照与快照配置比对";
    }

    @Override
    public String getToken() {
        return "inspect/config/snapshot/peer/compare";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "snapshotId", type = ApiParamType.LONG, isRequired = true, desc = "源快照id"),
            @Param(name = "targetSnapshotId", type = ApiParamType.LONG, isRequired = true, desc = "目标快照id")
    })
    @Output({
            @Param(name = "summary", type = ApiParamType.JSONOBJECT, desc = "比对汇总"),
            @Param(name = "diffList", type = ApiParamType.JSONARRAY, desc = "差异列表")
    })
    @Description(desc = "快照与快照配置比对")
    @Override
    public Object myDoService(JSONObject paramObj) {
        return inspectConfigCompareService.compareSnapshotPeer(paramObj.getLong("snapshotId"), paramObj.getLong("targetSnapshotId"));
    }
}

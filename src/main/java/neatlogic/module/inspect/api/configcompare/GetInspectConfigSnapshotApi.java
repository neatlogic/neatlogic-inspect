package neatlogic.module.inspect.api.configcompare;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.dto.InspectConfigSnapshotVo;
import neatlogic.module.inspect.service.InspectConfigCompareService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetInspectConfigSnapshotApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigCompareService inspectConfigCompareService;

    @Override
    public String getName() {
        return "获取巡检配置快照";
    }

    @Override
    public String getToken() {
        return "inspect/config/snapshot/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "快照id"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "应用模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "资源模型id"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资源id"),
            @Param(name = "schemaName", type = ApiParamType.STRING, desc = "采集维度")
    })
    @Output({
            @Param(name = "snapshot", type = ApiParamType.JSONOBJECT, desc = "快照"),
            @Param(name = "aiCandidate", type = ApiParamType.JSONOBJECT, desc = "AI候选结果"),
            @Param(name = "baselineDraft", type = ApiParamType.JSONOBJECT, desc = "AI辅助后的基线草稿"),
            @Param(name = "baselineSummary", type = ApiParamType.JSONOBJECT, desc = "基线草稿摘要"),
            @Param(name = "rawSnapshot", type = ApiParamType.JSONOBJECT, desc = "原始快照")
    })
    @Description(desc = "获取巡检配置快照")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        InspectConfigSnapshotVo snapshotVo;
        if (paramObj.getLong("id") != null) {
            return inspectConfigCompareService.getSnapshotDetail(paramObj.getLong("id"));
        } else {
            snapshotVo = inspectConfigCompareService.generateSnapshot(
                    paramObj.getLong("appSystemId"),
                    paramObj.getLong("appModuleId"),
                    paramObj.getLong("envId"),
                    paramObj.getLong("typeId"),
                    paramObj.getLong("resourceId"),
                    paramObj.getString("schemaName")
            );
        }
        JSONObject result = new JSONObject();
        result.put("snapshot", snapshotVo);
        return result;
    }
}

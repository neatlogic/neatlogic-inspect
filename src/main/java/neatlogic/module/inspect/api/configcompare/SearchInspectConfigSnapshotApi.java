package neatlogic.module.inspect.api.configcompare;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.inspect.dto.InspectConfigSnapshotVo;
import neatlogic.module.inspect.service.InspectConfigCompareService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchInspectConfigSnapshotApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigCompareService inspectConfigCompareService;

    @Override
    public String getName() {
        return "查询巡检配置快照列表";
    }

    @Override
    public String getToken() {
        return "inspect/config/snapshot/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "应用模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "资源模型id"),
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "资源id"),
            @Param(name = "schemaName", type = ApiParamType.STRING, isRequired = true, desc = "采集维度"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页条数")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, desc = "快照列表")
    })
    @Description(desc = "查询巡检配置快照列表")
    @Override
    public Object myDoService(JSONObject paramObj) {
        InspectConfigSnapshotVo searchVo = new InspectConfigSnapshotVo();
        searchVo.setAppSystemId(paramObj.getLong("appSystemId"));
        searchVo.setAppModuleId(paramObj.getLong("appModuleId"));
        searchVo.setEnvId(paramObj.getLong("envId"));
        searchVo.setTypeId(paramObj.getLong("typeId"));
        searchVo.setResourceId(paramObj.getLong("resourceId"));
        searchVo.setSchemaName(paramObj.getString("schemaName"));
        searchVo.setCurrentPage(paramObj.getInteger("currentPage"));
        searchVo.setPageSize(paramObj.getInteger("pageSize"));
        List<InspectConfigSnapshotVo> tbodyList = inspectConfigCompareService.getSnapshotList(searchVo);
        return TableResultUtil.getResult(tbodyList, searchVo);
    }
}

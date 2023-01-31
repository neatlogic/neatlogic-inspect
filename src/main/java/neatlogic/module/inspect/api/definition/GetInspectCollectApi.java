package neatlogic.module.inspect.api.definition;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.service.InspectCollectService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetInspectCollectApi extends PrivateApiComponentBase {

    @Resource
    private InspectCollectService inspectCollectService;

    @Override
    public String getName() {
        return "获得对应的集合";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "inspect/collection/get";
    }

    @Input({@Param(name = "name", type = ApiParamType.STRING, desc = "模型名称（唯一标识）")})
    @Output({
            @Param(name = "fields", type = ApiParamType.LONG, desc = "数据结构列表"),
            @Param(name = "thresholds", type = ApiParamType.LONG, desc = "阈值规则列表"),
            @Param(name = "userVo", explode = UserVo.class, desc = "修改人"),
            @Param(name = "lcd", type = ApiParamType.LONG, desc = "修改时间戳")
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return inspectCollectService.getCollectionByName(paramObj.getString("name"));
    }
}

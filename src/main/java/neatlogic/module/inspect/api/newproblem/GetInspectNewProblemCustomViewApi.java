package neatlogic.module.inspect.api.newproblem;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.inspect.dto.InspectNewProblemCustomViewVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.dao.mapper.InspectNewProblemCustomViewMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetInspectNewProblemCustomViewApi extends PrivateApiComponentBase {

    @Resource
    private InspectNewProblemCustomViewMapper inspectNewProblemCustomViewMapper;

    @Override
    public String getName() {
        return "获取巡检最新问题个人视图分类";
    }

    @Override
    public String getToken() {
        return "inspect/new/problem/customview/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "id", isRequired = true),
    })
    @Output({
            @Param(name = "Return", explode = InspectNewProblemCustomViewVo.class, type = ApiParamType.JSONOBJECT)
    })
    @Description(desc = "获取巡检最新问题个人视图分类")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return inspectNewProblemCustomViewMapper.getInspectNewProblemCustomViewById(paramObj.getLong("id"));
    }


}

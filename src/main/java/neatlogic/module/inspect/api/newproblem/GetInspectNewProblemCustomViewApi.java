package neatlogic.module.inspect.api.newproblem;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.inspect.dto.InspectNewProblemCustomViewVo;
import neatlogic.framework.inspect.exception.InspectNewProblemCustomViewNotFoundEditTargetException;
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
        return "nmian.getinspectnewproblemcustomviewapi.getname";
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
            @Param(name = "id", type = ApiParamType.LONG, desc = "common.id", isRequired = true),
    })
    @Output({
            @Param(name = "Return", explode = InspectNewProblemCustomViewVo.class, type = ApiParamType.JSONOBJECT)
    })
    @Description(desc = "nmian.getinspectnewproblemcustomviewapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        InspectNewProblemCustomViewVo inspectNewProblemCustomViewVo = inspectNewProblemCustomViewMapper.getInspectNewProblemCustomViewById(id);
        if (inspectNewProblemCustomViewVo == null) {
            throw new InspectNewProblemCustomViewNotFoundEditTargetException(id);
        }
        return inspectNewProblemCustomViewVo;
    }


}

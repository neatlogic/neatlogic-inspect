package neatlogic.module.inspect.api.newproblem;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.inspect.dto.InspectNewProblemCustomViewVo;
import neatlogic.framework.inspect.exception.InspectNewProblemCustomViewNameRepeatException;
import neatlogic.framework.inspect.exception.InspectNewProblemCustomViewNotFoundException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.dao.mapper.InspectNewProblemCustomViewMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class RenameInspectNewProblemCustomViewApi extends PrivateApiComponentBase {

    @Resource
    private InspectNewProblemCustomViewMapper inspectNewProblemCustomViewMapper;

    @Override
    public String getName() {
        return "重命名巡检最新问题个人视图分类";
    }

    @Override
    public String getToken() {
        return "inspect/new/problem/customview/rename";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "id", isRequired = true),
            @Param(name = "name", type = ApiParamType.STRING, desc = "名称", isRequired = true),
    })
    @Description(desc = "重命名巡检最新问题个人视图分类")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        InspectNewProblemCustomViewVo viewVo = paramObj.toJavaObject(InspectNewProblemCustomViewVo.class);
        viewVo.setUserUuid(UserContext.get().getUserUuid());
        InspectNewProblemCustomViewVo view = inspectNewProblemCustomViewMapper.getInspectNewProblemCustomViewById(viewVo.getId());
        if (view == null) {
            throw new InspectNewProblemCustomViewNotFoundException(viewVo.getId());
        }
        if (inspectNewProblemCustomViewMapper.checkInspectNewProblemCustomViewIsExists(viewVo) > 0) {
            throw new InspectNewProblemCustomViewNameRepeatException(viewVo.getName());
        }
        inspectNewProblemCustomViewMapper.updateInspectNewProblemCustomViewName(viewVo);
        return null;
    }


}

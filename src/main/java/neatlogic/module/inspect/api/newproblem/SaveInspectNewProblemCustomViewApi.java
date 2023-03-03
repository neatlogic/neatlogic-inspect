package neatlogic.module.inspect.api.newproblem;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.inspect.dto.InspectNewProblemCustomViewVo;
import neatlogic.framework.inspect.exception.InspectNewProblemCustomViewNameRepeatException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.dao.mapper.InspectNewProblemCustomViewMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class SaveInspectNewProblemCustomViewApi extends PrivateApiComponentBase {

    @Resource
    private InspectNewProblemCustomViewMapper inspectNewProblemCustomViewMapper;

    @Override
    public String getName() {
        return "保存巡检最新问题个人视图分类";
    }

    @Override
    public String getToken() {
        return "inspect/new/problem/customview/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "name", type = ApiParamType.STRING, desc = "名称", isRequired = true),
            @Param(name = "conditionConfig", type = ApiParamType.JSONOBJECT, desc = "条件", isRequired = true),
    })
    @Description(desc = "保存巡检最新问题个人视图分类")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        InspectNewProblemCustomViewVo viewVo = paramObj.toJavaObject(InspectNewProblemCustomViewVo.class);
        viewVo.setUserUuid(UserContext.get().getUserUuid());
        if (inspectNewProblemCustomViewMapper.checkInspectNewProblemCustomViewIsExists(viewVo) > 0) {
            throw new InspectNewProblemCustomViewNameRepeatException(viewVo.getName());
        }
        Integer sort = inspectNewProblemCustomViewMapper.getMaxSortByUserUuid(UserContext.get().getUserUuid());
        viewVo.setSort(sort != null ? sort + 1 : 1);
        inspectNewProblemCustomViewMapper.insertInspectNewProblemCustomView(viewVo);
        return viewVo.getId();
    }

    public IValid name() {
        return value -> {
            InspectNewProblemCustomViewVo vo = JSON.toJavaObject(value, InspectNewProblemCustomViewVo.class);
            vo.setUserUuid(UserContext.get().getUserUuid());
            if (inspectNewProblemCustomViewMapper.checkInspectNewProblemCustomViewIsExists(vo) > 0) {
                return new FieldValidResultVo(new InspectNewProblemCustomViewNameRepeatException(vo.getName()));
            }
            return new FieldValidResultVo();
        };
    }


}

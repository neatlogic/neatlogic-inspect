package codedriver.module.inspect.api.newproblem;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dto.InspectNewProblemCustomViewVo;
import codedriver.framework.inspect.exception.InspectNewProblemCustomViewNameRepeatException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.inspect.dao.mapper.InspectNewProblemCustomViewMapper;
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
        // todo 排序
        inspectNewProblemCustomViewMapper.insertInspectNewProblemCustomView(viewVo);
        return null;
    }

    public IValid name() {
        return value -> {
            InspectNewProblemCustomViewVo vo = JSON.toJavaObject(value, InspectNewProblemCustomViewVo.class);
            if (inspectNewProblemCustomViewMapper.checkInspectNewProblemCustomViewIsExists(vo) > 0) {
                return new FieldValidResultVo(new InspectNewProblemCustomViewNameRepeatException(vo.getName()));
            }
            return new FieldValidResultVo();
        };
    }


}

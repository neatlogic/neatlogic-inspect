package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dto.InspectNewProblemCustomViewVo;
import codedriver.framework.inspect.exception.InspectNewProblemCustomViewNameRepeatException;
import codedriver.framework.inspect.exception.InspectNewProblemCustomViewNotFoundException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.inspect.dao.mapper.InspectNewProblemCustomViewMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateInspectNewProblemCustomViewConditionApi extends PrivateApiComponentBase {

    @Resource
    private InspectNewProblemCustomViewMapper inspectNewProblemCustomViewMapper;

    @Override
    public String getName() {
        return "更新巡检最新问题个人视图分类搜索条件";
    }

    @Override
    public String getToken() {
        return "inspect/new/problem/customview/condition/update";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "id", isRequired = true),
            @Param(name = "conditionConfig", type = ApiParamType.JSONOBJECT, desc = "条件", isRequired = true),
    })
    @Description(desc = "更新巡检最新问题个人视图分类搜索条件")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        InspectNewProblemCustomViewVo viewVo = paramObj.toJavaObject(InspectNewProblemCustomViewVo.class);
        InspectNewProblemCustomViewVo view = inspectNewProblemCustomViewMapper.getInspectNewProblemCustomViewById(viewVo.getId());
        if (view == null) {
            throw new InspectNewProblemCustomViewNotFoundException(viewVo.getId());
        }
        inspectNewProblemCustomViewMapper.updateInspectNewProblemCustomViewCondition(viewVo);
        return null;
    }


}

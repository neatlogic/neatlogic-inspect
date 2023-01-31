package neatlogic.module.inspect.api.newproblem;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.inspect.dto.InspectNewProblemCustomViewVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.dao.mapper.InspectNewProblemCustomViewMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteInspectNewProblemCustomViewApi extends PrivateApiComponentBase {

    @Resource
    private InspectNewProblemCustomViewMapper inspectNewProblemCustomViewMapper;

    @Override
    public String getName() {
        return "删除巡检最新问题个人视图分类";
    }

    @Override
    public String getToken() {
        return "inspect/new/problem/customview/delete";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "id", isRequired = true),
    })
    @Description(desc = "删除巡检最新问题个人视图分类")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        InspectNewProblemCustomViewVo view = inspectNewProblemCustomViewMapper.getInspectNewProblemCustomViewById(id);
        inspectNewProblemCustomViewMapper.deleteInspectNewProblemCustomViewById(id);
        if (view != null) {
            inspectNewProblemCustomViewMapper.updateSortDecrement(UserContext.get().getUserUuid(), view.getSort(), null);
        }
        return null;
    }


}

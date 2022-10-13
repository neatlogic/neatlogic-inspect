/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api.newproblem;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dto.InspectNewProblemCustomViewVo;
import codedriver.framework.inspect.exception.InspectNewProblemCustomViewNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.inspect.dao.mapper.InspectNewProblemCustomViewMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class MoveInspectNewProblemCustomViewApi extends PrivateApiComponentBase {

    @Resource
    private InspectNewProblemCustomViewMapper inspectNewProblemCustomViewMapper;

    @Override
    public String getToken() {
        return "inspect/new/problem/customview/move";
    }

    @Override
    public String getName() {
        return "移动巡检最新问题个人视图分类";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "id"),
            @Param(name = "sort", type = ApiParamType.INTEGER, isRequired = true, desc = "移动后的序号")
    })
    @Output({
    })
    @Description(desc = "移动巡检最新问题个人视图分类")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        Integer newSort = jsonObj.getInteger("sort");
        InspectNewProblemCustomViewVo view = inspectNewProblemCustomViewMapper.getInspectNewProblemCustomViewById(id);
        if (view == null) {
            throw new InspectNewProblemCustomViewNotFoundException(id);
        }
        Integer oldSort = view.getSort();
        if (oldSort < newSort) { //往后移动
            inspectNewProblemCustomViewMapper.updateSortDecrement(UserContext.get().getUserUuid(), oldSort, newSort);
        } else if (oldSort > newSort) { //往前移动
            inspectNewProblemCustomViewMapper.updateSortIncrement(UserContext.get().getUserUuid(), newSort, oldSort);
        }
        view.setSort(newSort);
        inspectNewProblemCustomViewMapper.updateInspectNewProblemCustomViewSort(view);
        return null;
    }

}

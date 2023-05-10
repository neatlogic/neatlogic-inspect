/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.inspect.api.newproblem;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.inspect.dto.InspectNewProblemCustomViewVo;
import neatlogic.framework.inspect.exception.InspectNewProblemCustomViewNotFoundException;
import neatlogic.framework.restful.annotation.*;
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

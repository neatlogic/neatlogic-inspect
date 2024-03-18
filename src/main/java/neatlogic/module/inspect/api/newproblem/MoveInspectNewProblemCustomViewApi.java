/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.inspect.api.newproblem;

import com.alibaba.fastjson.JSONObject;
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

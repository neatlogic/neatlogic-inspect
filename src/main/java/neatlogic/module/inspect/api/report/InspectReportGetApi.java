/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.inspect.api.report;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.service.InspectReportService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Service
public class InspectReportGetApi extends PrivateApiComponentBase {

    @Resource
    private InspectReportService inspectReportService;

    @Override
    public String getName() {
        return "获取巡检报告";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资产id", isRequired = true),
            @Param(name = "id", type = ApiParamType.STRING, desc = "id"),
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id")
    })

    @Description(desc = "根据resourceId获取对应的巡检报告")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long resourceId = paramObj.getLong("resourceId");
        String id = paramObj.getString("id");
        Long jobId = paramObj.getLong("jobId");
        return inspectReportService.getInspectReport(resourceId, id, jobId);
    }

    @Override
    public String getToken() {
        return "inspect/report/get";
    }
}

/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

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

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class RefreshInspectAlertEverydayApi extends PrivateApiComponentBase {

    @Resource
    InspectReportService inspectReportService;

    @Override
    public String getName() {
        return "刷新巡检告警临时表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "startDate", type = ApiParamType.STRING, desc = "开始时间"),
            @Param(name = "endDate", type = ApiParamType.STRING, desc = "结束时间"),
    })
    @Description(desc = "刷新巡检告警临时表,如果startDate和endDate都为null，则默认获取前一天的数据")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        inspectReportService.updateInspectAlertEveryDayData(paramObj.getDate("startDate"), paramObj.getDate("endDate"));
        return null;
    }

    @Override
    public String getToken() {
        return "inspect/report/alert/everyday/refresh";
    }
}

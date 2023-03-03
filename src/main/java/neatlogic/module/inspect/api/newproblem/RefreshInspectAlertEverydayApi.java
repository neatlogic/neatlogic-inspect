/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

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
import com.alibaba.fastjson.JSONObject;
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

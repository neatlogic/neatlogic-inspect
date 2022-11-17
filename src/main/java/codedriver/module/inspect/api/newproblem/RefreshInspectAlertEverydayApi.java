/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api.newproblem;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.inspect.service.InspectReportService;
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
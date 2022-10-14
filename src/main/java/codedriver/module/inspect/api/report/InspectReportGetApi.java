/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api.report;

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

/*
 * Copyright (C) 2025 TechSure Co., Ltd. All Rights Reserved.
 */
package neatlogic.module.inspect.api.job;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.service.InspectAppJobService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 获取应用巡检父作业详情。
 */
@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetInspectAppJobDetailApi extends PrivateApiComponentBase {

    @Resource
    private InspectAppJobService inspectAppJobService;

    @Override
    public String getName() {
        return "nmiaj.getinspectappjobdetailapi.getname";
    }

    @Override
    public String getToken() {
        return "inspect/app/job/detail/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "parentJobId", type = ApiParamType.LONG, isRequired = true, desc = "term.inspect.parentjobid")
    })
    @Output({
            @Param(name = "parentJob", type = ApiParamType.JSONOBJECT, desc = "term.inspect.parentjob"),
            @Param(name = "childJobList", type = ApiParamType.JSONARRAY, desc = "term.inspect.childjoblist"),
            @Param(name = "summary", type = ApiParamType.JSONOBJECT, desc = "term.inspect.jobsummary"),
            @Param(name = "isCanAbort", type = ApiParamType.BOOLEAN, desc = "term.inspect.iscanabort"),
            @Param(name = "isCanCheck", type = ApiParamType.BOOLEAN, desc = "term.inspect.iscancheck"),
            @Param(name = "isCanExecute", type = ApiParamType.BOOLEAN, desc = "term.inspect.iscanexecute"),
            @Param(name = "reportReady", type = ApiParamType.BOOLEAN, desc = "term.inspect.reportready")
    })
    @Description(desc = "nmiaj.getinspectappjobdetailapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) {
        return inspectAppJobService.getDetail(paramObj.getLong("parentJobId"));
    }
}

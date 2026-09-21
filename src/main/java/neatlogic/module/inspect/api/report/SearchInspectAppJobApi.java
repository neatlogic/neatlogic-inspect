/*
 * Copyright (C) 2025 TechSure Co., Ltd. All Rights Reserved.
 */
package neatlogic.module.inspect.api.report;

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
 * 查询应用巡检父作业记录。
 */
@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchInspectAppJobApi extends PrivateApiComponentBase {

    @Resource
    private InspectAppJobService inspectAppJobService;

    @Override
    public String getName() {
        return "nmiar.searchinspectappjobapi.getname";
    }

    @Override
    public String getToken() {
        return "inspect/app/job/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.inspect.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "term.inspect.appmoduleid"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, desc = "term.inspect.inspectionrecordlist"),
            @Param(name = "rowNum", type = ApiParamType.LONG, desc = "common.rownum")
    })
    @Description(desc = "nmiar.searchinspectappjobapi.getname")
    /** 查询应用或模块范围内的巡检父作业。 */
    @Override
    public Object myDoService(JSONObject paramObj) {
        int currentPage = Math.max(1, paramObj.getIntValue("currentPage"));
        int pageSize = paramObj.getIntValue("pageSize");
        if (pageSize <= 0) {
            pageSize = 20;
        }
        return inspectAppJobService.search(paramObj.getLong("appSystemId"), paramObj.getLong("appModuleId"), currentPage, pageSize);
    }
}

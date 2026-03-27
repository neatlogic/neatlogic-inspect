package neatlogic.module.inspect.api.configcompare;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.service.InspectReportService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoCompareInspectConfigReportApi extends PrivateApiComponentBase {

    @Resource
    private InspectReportService inspectReportService;

    @Override
    public String getName() {
        return "巡检报告自动执行配置基线比对";
    }

    @Override
    public String getToken() {
        return "inspect/config/report/autocompare";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "资源id"),
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id")
    })
    @Description(desc = "巡检报告自动执行配置基线比对")
    @Override
    public Object myDoService(JSONObject paramObj) {
        inspectReportService.autoCompareConfigBaselineReport(paramObj.getLong("resourceId"), paramObj.getLong("jobId"));
        return null;
    }
}

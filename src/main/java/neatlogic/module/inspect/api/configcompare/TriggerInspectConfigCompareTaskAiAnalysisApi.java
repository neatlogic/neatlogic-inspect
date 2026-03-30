package neatlogic.module.inspect.api.configcompare;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.service.InspectConfigCompareService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class TriggerInspectConfigCompareTaskAiAnalysisApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigCompareService inspectConfigCompareService;

    @Override
    public String getName() {
        return "触发巡检配置比对AI分析";
    }

    @Override
    public String getToken() {
        return "inspect/config/compare/task/ai/analysis/trigger";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "taskId", type = ApiParamType.LONG, isRequired = true, desc = "比对任务id")
    })
    @Description(desc = "触发巡检配置比对AI分析")
    @Override
    public Object myDoService(JSONObject paramObj) {
        return inspectConfigCompareService.triggerCompareTaskAiAnalysis(paramObj.getLong("taskId"));
    }
}

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
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetInspectConfigCompareTaskApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigCompareService inspectConfigCompareService;

    @Override
    public String getName() {
        return "获取巡检配置比对任务详情";
    }

    @Override
    public String getToken() {
        return "inspect/config/compare/task/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "taskId", type = ApiParamType.LONG, isRequired = true, desc = "比对任务id")
    })
    @Description(desc = "获取巡检配置比对任务详情")
    @Override
    public Object myDoService(JSONObject paramObj) {
        return inspectConfigCompareService.getCompareTaskData(paramObj.getLong("taskId"));
    }
}

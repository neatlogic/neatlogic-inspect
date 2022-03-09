package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.crossover.IAutoexecServiceCrossoverService;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.auth.INSPECT_EXECUTE;
import codedriver.framework.inspect.auth.INSPECT_SCHEDULE_EXECUTE;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/1/24 5:54 下午
 */
@Service
@AuthAction(action = INSPECT_SCHEDULE_EXECUTE.class)
@AuthAction(action = INSPECT_EXECUTE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class InspectAutoexecJobSearchApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "查询巡检作业列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "inspect/autoexec/job/search";
    }

    @Input({
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "作业状态"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "组合工具类型"),
            @Param(name = "combopName", type = ApiParamType.STRING, desc = "组合工具"),
            @Param(name = "combopId", type = ApiParamType.LONG, desc = "组合工具Id"),
            @Param(name = "scheduleId", type = ApiParamType.LONG, desc = "组合工具定时作业Id"),
            @Param(name = "startTime", type = ApiParamType.JSONOBJECT, desc = "时间过滤"),
            @Param(name = "execUserList", type = ApiParamType.JSONARRAY, desc = "操作人"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecJobVo[].class, desc = "列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "巡检作业搜索（作业执行视图）")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecJobVo jobVo = new AutoexecJobVo(paramObj);
        List<String> sourceList = new ArrayList<>();
        sourceList.add("inspect");
        jobVo.setSourceList(sourceList);
        IAutoexecServiceCrossoverService iAutoexecServiceCrossoverService = CrossoverServiceFactory.getApi(IAutoexecServiceCrossoverService.class);
        return TableResultUtil.getResult(iAutoexecServiceCrossoverService.getJobList(jobVo), jobVo);
    }

}

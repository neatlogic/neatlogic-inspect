/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.inspect.api.job;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.crossover.IAutoexecJobCrossoverService;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.auth.INSPECT_EXECUTE;
import neatlogic.framework.inspect.auth.INSPECT_SCHEDULE_EXECUTE;
import neatlogic.framework.inspect.constvalue.JobSource;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.framework.util.TimeUtil;
import org.apache.commons.collections4.MapUtils;
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
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "id列表，用于精确查找作业刷新状态"),
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
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject startTimeJson = jsonObj.getJSONObject("startTime");
        if (MapUtils.isNotEmpty(startTimeJson)) {
            JSONObject timeJson = TimeUtil.getStartTimeAndEndTimeByDateJson(startTimeJson);
            jsonObj.put("startTime", timeJson.getDate("startTime"));
            jsonObj.put("endTime", timeJson.getDate("endTime"));
        }
        jsonObj.put("operationId", jsonObj.getLong("combopId"));
        jsonObj.put("invokeId", jsonObj.getLong("scheduleId"));
        AutoexecJobVo jobVo = JSONObject.toJavaObject(jsonObj, AutoexecJobVo.class);
        List<String> sourceList = new ArrayList<>();
        sourceList.add(JobSource.INSPECT.getValue());
        sourceList.add(JobSource.INSPECT_APP.getValue());
        sourceList.add(JobSource.SCHEDULE_INSPECT.getValue());
        sourceList.add(JobSource.SCHEDULE_INSPECT_APP.getValue());
        jobVo.setSourceList(sourceList);
        IAutoexecJobCrossoverService iAutoexecJobCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobCrossoverService.class);
        return TableResultUtil.getResult(iAutoexecJobCrossoverService.searchJob(jobVo), jobVo);
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

}

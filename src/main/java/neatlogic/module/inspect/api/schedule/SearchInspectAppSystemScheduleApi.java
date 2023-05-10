/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

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

package neatlogic.module.inspect.api.schedule;

import com.alibaba.fastjson.JSONArray;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobInvokeVo;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AppSystemVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.inspect.dao.mapper.InspectScheduleMapper;
import neatlogic.framework.inspect.dto.InspectAppSystemScheduleVo;
import neatlogic.framework.inspect.dto.InspectScheduleVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchInspectAppSystemScheduleApi extends PrivateApiComponentBase {

    @Resource
    private InspectScheduleMapper inspectScheduleMapper;

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getToken() {
        return "inspect/appsystem/schedule/search";
    }

    @Override
    public String getName() {
        return "查询应用定时巡检任务列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "默认值"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = InspectScheduleVo[].class)
    })
    @Description(desc = "查询应用定时巡检任务列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        BasePageVo searchVo = paramObj.toJavaObject(BasePageVo.class);
        List<AppSystemVo> appSystemList = new ArrayList<>();
        List<InspectAppSystemScheduleVo> inspectAppSystemScheduleList = new ArrayList<>();
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            inspectAppSystemScheduleList = inspectScheduleMapper.getInspectAppSystemScheduleListByIdList(idList);
            List<Long> appSystemIdList = inspectAppSystemScheduleList.stream().map(InspectAppSystemScheduleVo::getAppSystemId).collect(Collectors.toList());
            appSystemList = resourceCrossoverMapper.getAppSystemListByIdList(appSystemIdList);
        } else {
            int rowNum = resourceCrossoverMapper.getAppSystemCountByKeyword(searchVo);
            if (rowNum == 0) {
                return TableResultUtil.getResult(new ArrayList<>(), searchVo);
            }
            searchVo.setRowNum(rowNum);
            appSystemList = resourceCrossoverMapper.getAppSystemListByKeyword(searchVo);
            if (CollectionUtils.isEmpty(appSystemList)) {
                return TableResultUtil.getResult(new ArrayList<>(), searchVo);
            }
            List<Long> appSystemIdList = appSystemList.stream().map(AppSystemVo::getId).collect(Collectors.toList());
            inspectAppSystemScheduleList = inspectScheduleMapper.getInspectAppSystemScheduleListByAppSystemIdList(appSystemIdList);
        }

        Map<Long, InspectAppSystemScheduleVo> inspectAppSystemScheduleMap = inspectAppSystemScheduleList.stream().collect(Collectors.toMap(e -> e.getAppSystemId(), e -> e));
        List<Long> scheduleIdList = inspectAppSystemScheduleList.stream().map(InspectAppSystemScheduleVo::getId).collect(Collectors.toList());
        Map<Long, Integer> execCountMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(scheduleIdList)) {
            List<AutoexecJobInvokeVo> execCountList = autoexecJobMapper.getJobIdCountListByInvokeIdList(scheduleIdList);
            execCountMap = execCountList.stream().collect(Collectors.toMap(AutoexecJobInvokeVo::getInvokeId, AutoexecJobInvokeVo::getCount));
        }
        List<InspectAppSystemScheduleVo> tbodyList = new ArrayList<>(appSystemList.size());
        for (AppSystemVo appSystemVo : appSystemList) {
            InspectAppSystemScheduleVo inspectAppSystemScheduleVo = inspectAppSystemScheduleMap.get(appSystemVo.getId());
            if (inspectAppSystemScheduleVo == null) {
                inspectAppSystemScheduleVo = new InspectAppSystemScheduleVo();
            } else {
                Integer execCount = execCountMap.get(inspectAppSystemScheduleVo.getId());
                if (execCount != null && execCount > 0) {
                    inspectAppSystemScheduleVo.setExecCount(execCount);
                }
            }
            inspectAppSystemScheduleVo.setAppSystemId(appSystemVo.getId());
            inspectAppSystemScheduleVo.setAppSystemName(appSystemVo.getName());
            inspectAppSystemScheduleVo.setAppSystemAbbrName(appSystemVo.getAbbrName());
            tbodyList.add(inspectAppSystemScheduleVo);
        }
        return TableResultUtil.getResult(tbodyList, searchVo);
    }
}

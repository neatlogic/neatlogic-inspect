/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.inspect.job.source.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.dto.job.AutoexecJobRouteVo;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.constvalue.JobSource;
import neatlogic.framework.inspect.dao.mapper.InspectScheduleMapper;
import neatlogic.framework.inspect.dto.InspectAppSystemScheduleVo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ScheduleInspectAppJobSourceHandler implements IAutoexecJobSource {

    @Resource
    private InspectScheduleMapper inspectScheduleMapper;
    @Override
    public String getValue() {
        return JobSource.SCHEDULE_INSPECT_APP.getValue();
    }

    @Override
    public String getText() {
        return JobSource.SCHEDULE_INSPECT_APP.getText();
    }

    @Override
    public List<AutoexecJobRouteVo> getListByUniqueKeyList(List<String> uniqueKeyList) {
        if (CollectionUtils.isEmpty(uniqueKeyList)) {
            return null;
        }
        List<Long> idList = new ArrayList<>();
        for (String str : uniqueKeyList) {
            idList.add(Long.valueOf(str));
        }
        List<AutoexecJobRouteVo> resultList = new ArrayList<>();
        List<InspectAppSystemScheduleVo> list = inspectScheduleMapper.getInspectAppSystemScheduleListByIdList(idList);
        Set<Long> appSystemIdSet = list.stream().map(InspectAppSystemScheduleVo::getAppSystemId).collect(Collectors.toSet());
        IAppSystemMapper appSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        List<AppSystemVo> appSystemList = appSystemMapper.getAppSystemListByIdList(new ArrayList<>(appSystemIdSet));
        Map<Long, AppSystemVo> appSystemMap = appSystemList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        for (InspectAppSystemScheduleVo scheduleVo : list) {
            JSONObject config = new JSONObject();
            config.put("scheduleId", scheduleVo.getId());
            config.put("appSystemId", scheduleVo.getAppSystemId());
            AppSystemVo appSystemVo = appSystemMap.get(scheduleVo.getAppSystemId());
            String label = "";
            if (appSystemVo != null) {
                if (StringUtils.isNotBlank(appSystemVo.getAbbrName()) && StringUtils.isNotBlank(appSystemVo.getName())) {
                    label = appSystemVo.getAbbrName() + "(" + appSystemVo.getName() + ")";
                } else if (StringUtils.isNotBlank(appSystemVo.getAbbrName())) {
                    label = appSystemVo.getAbbrName();
                } else if (StringUtils.isNotBlank(appSystemVo.getName())) {
                    label = appSystemVo.getName();
                }
                scheduleVo.setAppSystemName(appSystemVo.getName());
                scheduleVo.setAppSystemAbbrName(appSystemVo.getAbbrName());
            }
            resultList.add(new AutoexecJobRouteVo(scheduleVo.getId(), label, config));
        }
        return resultList;
    }
}

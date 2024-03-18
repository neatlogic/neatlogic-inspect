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

package neatlogic.module.inspect.job.source.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.dto.job.AutoexecJobRouteVo;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AppSystemVo;
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
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        List<AppSystemVo> appSystemList = resourceCrossoverMapper.getAppSystemListByIdList(new ArrayList<>(appSystemIdSet));
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

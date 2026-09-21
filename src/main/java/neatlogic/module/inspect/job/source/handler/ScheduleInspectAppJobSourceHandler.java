/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.inspect.job.source.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.dto.job.AutoexecJobRouteVo;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AppSystemVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.constvalue.JobSource;
import neatlogic.framework.inspect.dao.mapper.InspectScheduleMapper;
import neatlogic.framework.inspect.dto.InspectAppSystemScheduleVo;
import neatlogic.module.inspect.job.source.InspectAppJobRouteKey;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
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
        List<AutoexecJobRouteVo> resultList = new ArrayList<>();
        Map<Long, List<String>> scheduleIdRouteKeyMap = new LinkedHashMap<>();
        Map<Long, List<String>> ciIdRouteKeyMap = new LinkedHashMap<>();
        for (String routeKey : uniqueKeyList) {
            if (routeKey.startsWith(InspectAppJobRouteKey.CI_PREFIX)) {
                Long ciId = InspectAppJobRouteKey.parse(routeKey, InspectAppJobRouteKey.CI_PREFIX);
                ciIdRouteKeyMap.computeIfAbsent(ciId, key -> new ArrayList<>()).add(routeKey);
            } else {
                Long scheduleId = routeKey.startsWith(InspectAppJobRouteKey.SCHEDULE_PREFIX)
                        ? InspectAppJobRouteKey.parse(routeKey, InspectAppJobRouteKey.SCHEDULE_PREFIX)
                        : Long.valueOf(routeKey);
                scheduleIdRouteKeyMap.computeIfAbsent(scheduleId, key -> new ArrayList<>()).add(routeKey);
            }
        }
        appendScheduleRouteList(resultList, scheduleIdRouteKeyMap);
        appendCiRouteList(resultList, ciIdRouteKeyMap);
        return resultList;
    }

    /**
     * 补充定时任务对应的应用来源类目，纯数字键用于兼容历史作业。
     */
    private void appendScheduleRouteList(List<AutoexecJobRouteVo> resultList, Map<Long, List<String>> idRouteKeyMap) {
        if (idRouteKeyMap.isEmpty()) {
            return;
        }
        List<InspectAppSystemScheduleVo> scheduleList = inspectScheduleMapper.getInspectAppSystemScheduleListByIdList(new ArrayList<>(idRouteKeyMap.keySet()));
        if (CollectionUtils.isEmpty(scheduleList)) {
            return;
        }
        Set<Long> appSystemIdSet = scheduleList.stream().map(InspectAppSystemScheduleVo::getAppSystemId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(appSystemIdSet)) {
            return;
        }
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        List<AppSystemVo> appSystemList = resourceCrossoverMapper.getAppSystemListByIdList(new ArrayList<>(appSystemIdSet));
        Map<Long, AppSystemVo> appSystemMap = appSystemList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        for (InspectAppSystemScheduleVo scheduleVo : scheduleList) {
            JSONObject config = new JSONObject();
            config.put("scheduleId", scheduleVo.getId());
            config.put("appSystemId", scheduleVo.getAppSystemId());
            AppSystemVo appSystemVo = appSystemMap.get(scheduleVo.getAppSystemId());
            if (appSystemVo == null) {
                continue;
            }
            for (String routeKey : idRouteKeyMap.get(scheduleVo.getId())) {
                resultList.add(new AutoexecJobRouteVo(routeKey, getAppSystemLabel(appSystemVo), config));
            }
        }
    }

    /**
     * 补充模型来源类目。
     */
    private void appendCiRouteList(List<AutoexecJobRouteVo> resultList, Map<Long, List<String>> idRouteKeyMap) {
        if (idRouteKeyMap.isEmpty()) {
            return;
        }
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        List<CiVo> ciList = ciCrossoverMapper.getCiByIdList(new ArrayList<>(idRouteKeyMap.keySet()));
        for (CiVo ciVo : ciList) {
            JSONObject config = new JSONObject();
            config.put("ciId", ciVo.getId());
            for (String routeKey : idRouteKeyMap.get(ciVo.getId())) {
                resultList.add(new AutoexecJobRouteVo(routeKey, ciVo.getLabel() + "(" + ciVo.getName() + ")", config));
            }
        }
    }

    /**
     * 生成应用展示名称。
     */
    private String getAppSystemLabel(AppSystemVo appSystemVo) {
        if (StringUtils.isNotBlank(appSystemVo.getAbbrName()) && StringUtils.isNotBlank(appSystemVo.getName())) {
            return appSystemVo.getAbbrName() + "(" + appSystemVo.getName() + ")";
        } else if (StringUtils.isNotBlank(appSystemVo.getAbbrName())) {
            return appSystemVo.getAbbrName();
        }
        return StringUtils.defaultString(appSystemVo.getName());
    }
}

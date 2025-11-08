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
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AppSystemVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.constvalue.JobSource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class InspectAppJobSourceHandler implements IAutoexecJobSource {

    @Override
    public String getValue() {
        return JobSource.INSPECT_APP.getValue();
    }

    @Override
    public String getText() {
        return JobSource.INSPECT_APP.getText();
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
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        List<AppSystemVo> list = resourceCrossoverMapper.getAppSystemListByIdList(idList);
        for (AppSystemVo appSystemVo : list) {
            JSONObject config = new JSONObject();
            config.put("id", appSystemVo.getId());
            String label = "";
            if (appSystemVo != null) {
                if (StringUtils.isNotBlank(appSystemVo.getAbbrName()) && StringUtils.isNotBlank(appSystemVo.getName())) {
                    label = appSystemVo.getAbbrName() + "(" + appSystemVo.getName() + ")";
                } else if (StringUtils.isNotBlank(appSystemVo.getAbbrName())) {
                    label = appSystemVo.getAbbrName();
                } else if (StringUtils.isNotBlank(appSystemVo.getName())) {
                    label = appSystemVo.getName();
                }
            }
            resultList.add(new AutoexecJobRouteVo(appSystemVo.getId(), label, config));
        }
        return resultList;
    }
}

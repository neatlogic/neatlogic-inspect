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
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.constvalue.JobSource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class InspectJobSourceHandler implements IAutoexecJobSource {

    @Override
    public String getValue() {
        return JobSource.INSPECT.getValue();
    }

    @Override
    public String getText() {
        return JobSource.INSPECT.getText();
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
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        List<CiVo> list = ciCrossoverMapper.getCiByIdList(idList);
        for (CiVo ciVo : list) {
            JSONObject config = new JSONObject();
            config.put("id", ciVo.getId());
            resultList.add(new AutoexecJobRouteVo(ciVo.getId(), ciVo.getLabel() + "(" + ciVo.getName() + ")", config));
        }
        return resultList;
    }
}

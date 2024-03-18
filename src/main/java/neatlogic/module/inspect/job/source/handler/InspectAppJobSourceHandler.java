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

/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.inspect.api.configfile;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.inspect.dto.InspectConfigFilePathSearchVo;
import neatlogic.framework.inspect.dto.InspectConfigFilePathVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.inspect.dao.mapper.InspectConfigFileMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListInspectConfigFilePathApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigFileMapper inspectConfigFileMapper;

    @Override
    public String getToken() {
        return "inspect/configfile/path/list";
    }

    @Override
    public String getName() {
        return "巡检配置文件路径列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊匹配文件名、资产IP、资产名称"),
            @Param(name = "timeRange", type = ApiParamType.JSONARRAY, desc = "最近变更时间"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", explode = InspectConfigFilePathVo[].class, desc = "文件路径列表")
    })
    @Description(desc = "巡检配置文件资源文件路径列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        InspectConfigFilePathSearchVo searchVo = JSONObject.toJavaObject(paramObj, InspectConfigFilePathSearchVo.class);
        int rowNum = inspectConfigFileMapper.getInspectConfigFilePathCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            List<Long> idList = inspectConfigFileMapper.getInspectConfigFilePathIdList(searchVo);
            if (CollectionUtils.isNotEmpty(idList)) {
                List<InspectConfigFilePathVo> tbodyList = new ArrayList<>();
                List<InspectConfigFilePathVo> inspectResourceConfigFilePathList = inspectConfigFileMapper.getInspectConfigFilePathList(idList);
                Map<Long, InspectConfigFilePathVo> map = inspectResourceConfigFilePathList.stream().collect(Collectors.toMap(InspectConfigFilePathVo::getId, e -> e));
                List<InspectConfigFilePathVo> inspectResourceConfigFileVersionCountList = inspectConfigFileMapper.getInspectConfigFileVersionCountByPathIdList(idList);
                Map<Long, Integer> versionCountMap = inspectResourceConfigFileVersionCountList.stream().collect(Collectors.toMap(InspectConfigFilePathVo::getId, InspectConfigFilePathVo::getVersionCount));
                for (Long id : idList) {
                    InspectConfigFilePathVo pathVo = map.get(id);
                    Integer versionCount = versionCountMap.get(pathVo.getId());
                    if (versionCount != null) {
                        pathVo.setVersionCount(versionCount);
                    }
                    tbodyList.add(pathVo);
                }
                return TableResultUtil.getResult(tbodyList, searchVo);
            }

        }
        return TableResultUtil.getResult(new ArrayList<>(), searchVo);
    }
}

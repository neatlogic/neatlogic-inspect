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
import neatlogic.module.inspect.service.InspectService;
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

    @Resource
    private InspectService inspectService;

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
        int rowNum = inspectService.getInspectConfigFilePathCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            List<Long> idList = inspectService.getInspectConfigFilePathIdList(searchVo);
            if (CollectionUtils.isNotEmpty(idList)) {
                List<InspectConfigFilePathVo> tbodyList = new ArrayList<>();
                List<InspectConfigFilePathVo> inspectResourceConfigFilePathList = inspectService.getInspectConfigFilePathList(idList);
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

/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

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

package neatlogic.module.inspect.api.configfile;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.inspect.dto.InspectConfigFilePathVo;
import neatlogic.framework.inspect.dto.InspectConfigFileVersionVo;
import neatlogic.framework.inspect.exception.InspectConfigFilePathNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.inspect.dao.mapper.InspectConfigFileMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListInspectConfigFileVersionApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigFileMapper inspectConfigFileMapper;

    @Override
    public String getToken() {
        return "inspect/configfile/resource/version/list";
    }

    @Override
    public String getName() {
        return "巡检配置文件资源文件变更记录列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "pathId", type = ApiParamType.LONG, isRequired = true, desc = "资源文件路径id"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显的资源ID列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", explode = InspectConfigFilePathVo[].class, desc = "文件变更记录列表")
    })
    @Description(desc = "巡检配置文件资源文件变更记录列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        InspectConfigFileVersionVo searchVo = paramObj.toJavaObject(InspectConfigFileVersionVo.class);
        Long pathId = searchVo.getPathId();
        InspectConfigFilePathVo inspectConfigFilePathVo = inspectConfigFileMapper.getInspectConfigFilePathById(pathId);
        if (inspectConfigFilePathVo == null) {
            throw new InspectConfigFilePathNotFoundException(pathId);
        }
        List<InspectConfigFileVersionVo> tbodyList = new ArrayList<>();
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            tbodyList = inspectConfigFileMapper.getInspectConfigFileVersionListByIdList(idList);
        } else {
            int rowNum = inspectConfigFileMapper.getInspectConfigFileVersionCountByPathId(pathId);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                Boolean needPage = paramObj.getBoolean("needPage");
                needPage = needPage == null ? true : needPage;
                if (needPage) {
                    List<Long> idList = inspectConfigFileMapper.getInspectConfigFileVersionIdListByPathId(searchVo);
                    if (CollectionUtils.isNotEmpty(idList)) {
                        tbodyList = inspectConfigFileMapper.getInspectConfigFileVersionListByIdList(idList);
                    }
                } else {
                    int pageCount = searchVo.getPageCount();
                    for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
                        searchVo.setCurrentPage(currentPage);
                        List<Long> idList = inspectConfigFileMapper.getInspectConfigFileVersionIdListByPathId(searchVo);
                        List<InspectConfigFileVersionVo> inspectResourceConfigFileVersionList = inspectConfigFileMapper.getInspectConfigFileVersionListByIdList(idList);
                        tbodyList.addAll(inspectResourceConfigFileVersionList);
                    }
                }
            }
        }
        return TableResultUtil.getResult(tbodyList, searchVo);
    }
}

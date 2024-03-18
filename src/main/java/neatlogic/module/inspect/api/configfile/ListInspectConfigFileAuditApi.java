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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.inspect.dto.InspectConfigFileAuditVo;
import neatlogic.framework.inspect.dto.InspectConfigFilePathVo;
import neatlogic.framework.inspect.exception.InspectConfigFilePathNotFoundException;
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

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListInspectConfigFileAuditApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigFileMapper inspectConfigFileMapper;

    @Override
    public String getToken() {
        return "inspect/configfile/resource/audit/list";
    }

    @Override
    public String getName() {
        return "巡检配置文件资源文件扫描历史列表";
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
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = InspectConfigFilePathVo[].class, desc = "文件扫描历史列表")
    })
    @Description(desc = "巡检配置文件资源文件扫描历史列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        InspectConfigFileAuditVo searchVo = paramObj.toJavaObject(InspectConfigFileAuditVo.class);
        Long pathId = searchVo.getPathId();
        InspectConfigFilePathVo inspectConfigFilePathVo = inspectConfigFileMapper.getInspectConfigFilePathById(pathId);
        if (inspectConfigFilePathVo == null) {
            throw new InspectConfigFilePathNotFoundException(pathId);
        }
        List<InspectConfigFileAuditVo> tbodyList = new ArrayList<>();
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            tbodyList = inspectConfigFileMapper.getInspectConfigFileAuditListByIdList(idList);
        } else {
            int rowNum = inspectConfigFileMapper.getInspectConfigFileAuditCountByPathId(pathId);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                Boolean needPage = paramObj.getBoolean("needPage");
                needPage = needPage == null ? true : needPage;
                if (needPage) {
                    List<Long> idList = inspectConfigFileMapper.getInspectConfigFileAuditIdListByPathId(searchVo);
                    if (CollectionUtils.isNotEmpty(idList)) {
                        tbodyList = inspectConfigFileMapper.getInspectConfigFileAuditListByIdList(idList);
                    }
                } else {
                    int pageCount = searchVo.getPageCount();
                    for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
                        searchVo.setCurrentPage(currentPage);
                        List<Long> idList = inspectConfigFileMapper.getInspectConfigFileAuditIdListByPathId(searchVo);
                        List<InspectConfigFileAuditVo> inspectResourceConfigFileRecordList = inspectConfigFileMapper.getInspectConfigFileAuditListByIdList(idList);
                        tbodyList.addAll(inspectResourceConfigFileRecordList);
                    }
                }
            }
        }
        return TableResultUtil.getResult(tbodyList, searchVo);
    }
}

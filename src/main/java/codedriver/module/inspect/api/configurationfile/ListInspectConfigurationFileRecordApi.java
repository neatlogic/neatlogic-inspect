/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api.configurationfile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dto.InspectResourceConfigurationFilePathVo;
import codedriver.framework.inspect.dto.InspectResourceConfigurationFileRecordVo;
import codedriver.framework.inspect.exception.InspectResourceConfigurationFilePathNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.inspect.dao.mapper.InspectConfigurationFileMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListInspectConfigurationFileRecordApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigurationFileMapper inspectConfigurationFileMapper;

    @Override
    public String getToken() {
        return "inspect/configurationfile/resource/record/list";
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
            @Param(name = "tbodyList", explode = InspectResourceConfigurationFilePathVo[].class, desc = "文件扫描历史列表")
    })
    @Description(desc = "巡检配置文件资源文件扫描历史列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        InspectResourceConfigurationFileRecordVo searchVo = paramObj.toJavaObject(InspectResourceConfigurationFileRecordVo.class);
        Long pathId = searchVo.getPathId();
        InspectResourceConfigurationFilePathVo inspectResourceConfigurationFilePathVo = inspectConfigurationFileMapper.getInspectResourceConfigurationFilePathById(pathId);
        if (inspectResourceConfigurationFilePathVo == null) {
            throw new InspectResourceConfigurationFilePathNotFoundException(pathId);
        }
        List<InspectResourceConfigurationFileRecordVo> tbodyList = new ArrayList<>();
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            tbodyList = inspectConfigurationFileMapper.getInspectResourceConfigurationFileRecordListByIdList(idList);
        } else {
            int rowNum = inspectConfigurationFileMapper.getInspectResourceConfigurationFileRecordCountByPathId(pathId);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                Boolean needPage = paramObj.getBoolean("needPage");
                needPage = needPage == null ? true : needPage;
                if (needPage) {
                    List<Long> idList = inspectConfigurationFileMapper.getInspectResourceConfigurationFileRecordIdListByPathId(searchVo);
                    if (CollectionUtils.isNotEmpty(idList)) {
                        tbodyList = inspectConfigurationFileMapper.getInspectResourceConfigurationFileRecordListByIdList(idList);
                    }
                } else {
                    int pageCount = searchVo.getPageCount();
                    for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
                        searchVo.setCurrentPage(currentPage);
                        List<Long> idList = inspectConfigurationFileMapper.getInspectResourceConfigurationFileRecordIdListByPathId(searchVo);
                        List<InspectResourceConfigurationFileRecordVo> inspectResourceConfigurationFileRecordList = inspectConfigurationFileMapper.getInspectResourceConfigurationFileRecordListByIdList(idList);
                        tbodyList.addAll(inspectResourceConfigurationFileRecordList);
                    }
                }
            }
        }
        return TableResultUtil.getResult(tbodyList, searchVo);
    }
}

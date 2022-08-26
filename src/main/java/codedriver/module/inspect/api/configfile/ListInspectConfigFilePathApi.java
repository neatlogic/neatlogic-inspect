/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api.configfile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dto.InspectConfigFilePathSearchVo;
import codedriver.framework.inspect.dto.InspectConfigFilePathVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.inspect.dao.mapper.InspectConfigFileMapper;
import com.alibaba.fastjson.JSONObject;
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
            List<InspectConfigFilePathVo> inspectResourceConfigFilePathList = inspectConfigFileMapper.getInspectConfigFilePathList(searchVo);
            List<Long> idList = inspectResourceConfigFilePathList.stream().map(InspectConfigFilePathVo::getId).collect(Collectors.toList());
            List<InspectConfigFilePathVo> inspectResourceConfigFileVersionCountList = inspectConfigFileMapper.getInspectConfigFileVersionCountByPathIdList(idList);
            Map<Long, Integer> versionCountMap = inspectResourceConfigFileVersionCountList.stream().collect(Collectors.toMap(InspectConfigFilePathVo::getId, InspectConfigFilePathVo::getVersionCount));
            for (InspectConfigFilePathVo pathVo : inspectResourceConfigFilePathList) {
                Integer versionCount = versionCountMap.get(pathVo.getId());
                if (versionCount != null) {
                    pathVo.setVersionCount(versionCount);
                }
            }
            return TableResultUtil.getResult(inspectResourceConfigFilePathList, searchVo);
        }
        return TableResultUtil.getResult(new ArrayList<>(), searchVo);
    }
}

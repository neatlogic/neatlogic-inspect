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
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.auth.INSPECT_CONFIG_FILE_MODIFY;
import neatlogic.framework.inspect.dao.mapper.InspectMapper;
import neatlogic.framework.inspect.dto.InspectConfigFilePathVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.dao.mapper.InspectConfigFileMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@AuthAction(action = INSPECT_CONFIG_FILE_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class BatchAddInspectConfigFileResourcePathApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigFileMapper inspectConfigFileMapper;
    @Resource
    private InspectMapper inspectMapper;

    @Override
    public String getToken() {
        return "inspect/configfile/resource/path/batchadd";
    }

    @Override
    public String getName() {
        return "批量添加巡检配置文件资源路径";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "模糊搜索"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true, desc = "类型id"),
            @Param(name = "protocolIdList", type = ApiParamType.JSONARRAY, desc = "协议id列表"),
            @Param(name = "stateIdList", type = ApiParamType.JSONARRAY, desc = "状态id列表"),
            @Param(name = "vendorIdList", type = ApiParamType.JSONARRAY, desc = "厂商id列表"),
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, desc = "环境id列表"),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "应用系统id列表"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "资源类型id列表"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, desc = "标签id列表"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显的资源ID列表"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "巡检状态列表"),
            @Param(name = "inspectJobPhaseNodeStatusList", type = ApiParamType.JSONARRAY, desc = "巡检作业状态列表"),
            @Param(name = "pathList", type = ApiParamType.JSONARRAY, desc = "路径列表")
    })
    @Output({})
    @Description(desc = "批量添加巡检配置文件资源路径")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray pathArray = paramObj.getJSONArray("pathList");
        if (CollectionUtils.isEmpty(pathArray)) {
            return null;
        }
        ResourceSearchVo searchVo = JSONObject.toJavaObject(paramObj, ResourceSearchVo.class);
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> resourceIdList = defaultValue.toJavaList(Long.class);
            ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
            List<CiEntityVo> ciEntityList = ciEntityCrossoverMapper.getCiEntityBaseInfoByIdList(resourceIdList);
            resourceIdList = ciEntityList.stream().map(CiEntityVo::getId).collect(Collectors.toList());
            addPath(resourceIdList, pathArray);
        } else {
            Long typeId = searchVo.getTypeId();
            ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
            CiVo ciVo = ciCrossoverMapper.getCiById(typeId);
            if (ciVo == null) {
                throw new CiNotFoundException(typeId);
            }
            searchVo.setLft(ciVo.getLft());
            searchVo.setRht(ciVo.getRht());
            IResourceCenterResourceCrossoverService resourceCenterResourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
            List<Long> typeIdList = resourceCenterResourceCrossoverService.getDownwardCiIdListByCiIdList(Arrays.asList(searchVo.getTypeId()));
            searchVo.setTypeIdList(typeIdList);
            int count = inspectMapper.getInspectResourceCount(searchVo);
            if (count > 0) {
                searchVo.setPageSize(100);
                searchVo.setRowNum(count);
                int pageCount = searchVo.getPageCount();
                for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
                    searchVo.setCurrentPage(currentPage);
                    List<Long> resourceIdList = inspectMapper.getInspectResourceIdList(searchVo);
                    addPath(resourceIdList, pathArray);
                }
            }
        }
        return null;
    }

    /**
     * 批量添加路径
     *
     * @param resourceIdList 资源id列表
     * @param pathArray      路径列表
     */
    private void addPath(List<Long> resourceIdList, JSONArray pathArray) {
        Map<Long, List<InspectConfigFilePathVo>> inspectResourceConfigFilePathMap = new HashMap<>();
        List<InspectConfigFilePathVo> inspectResourceConfigFilePathList = inspectConfigFileMapper.getInspectConfigFilePathListByResourceIdList(resourceIdList);
        for (InspectConfigFilePathVo pathVo : inspectResourceConfigFilePathList) {
            Long resourceId = pathVo.getResourceId();
            inspectResourceConfigFilePathMap.computeIfAbsent(resourceId, key -> new ArrayList<>()).add(pathVo);
        }
        for (Long resourceId : resourceIdList) {
            List<String> oldPathList = new ArrayList<>();
            inspectResourceConfigFilePathList = inspectResourceConfigFilePathMap.get(resourceId);
            if (CollectionUtils.isNotEmpty(inspectResourceConfigFilePathList)) {
                oldPathList = inspectResourceConfigFilePathList.stream().map(InspectConfigFilePathVo::getPath).collect(Collectors.toList());
            }
            List<String> pathList = pathArray.toJavaList(String.class);
            List<String> needInsertPathList = ListUtils.removeAll(pathList, oldPathList);
            for (String path : needInsertPathList) {
                InspectConfigFilePathVo pathVo = new InspectConfigFilePathVo(resourceId, path);
                inspectConfigFileMapper.insertInspectConfigFilePath(pathVo);
            }
        }
    }
}

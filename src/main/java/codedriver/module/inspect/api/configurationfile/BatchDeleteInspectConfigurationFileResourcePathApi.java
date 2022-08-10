/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api.configurationfile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiCrossoverMapper;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dao.mapper.InspectMapper;
import codedriver.framework.inspect.dto.InspectResourceConfigurationFilePathVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.inspect.dao.mapper.InspectConfigurationFileMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class BatchDeleteInspectConfigurationFileResourcePathApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigurationFileMapper inspectConfigurationFileMapper;
    @Resource
    private InspectMapper inspectMapper;

    @Override
    public String getToken() {
        return "inspect/configurationfile/resource/path/batchdelete";
    }

    @Override
    public String getName() {
        return "批量删除巡检配置文件资源路径";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "模糊搜索"),
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "id列表，用于刷新状态时精确匹配数据"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true, desc = "类型id"),
            @Param(name = "protocolIdList", type = ApiParamType.JSONARRAY, desc = "协议id列表"),
            @Param(name = "stateIdList", type = ApiParamType.JSONARRAY, desc = "状态id列表"),
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
    @Output({

    })
    @Description(desc = "批量删除巡检配置文件资源路径")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray pathArray = paramObj.getJSONArray("pathList");
        if (CollectionUtils.isEmpty(pathArray)) {
            return null;
        }
        ResourceSearchVo searchVo = JSONObject.toJavaObject(paramObj, ResourceSearchVo.class);
        JSONArray defaultVaule = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultVaule)) {
            List<Long> resourceIdList = defaultVaule.toJavaList(Long.class);
            ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
            List<CiEntityVo> ciEntityList = ciEntityCrossoverMapper.getCiEntityBaseInfoByIdList(resourceIdList);
            resourceIdList = ciEntityList.stream().map(CiEntityVo::getId).collect(Collectors.toList());
            deletePath(resourceIdList, pathArray);
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
                    deletePath(resourceIdList, pathArray);
                }
            }
        }
        return null;
    }

    /**
     * 批量删除路径
     * @param resourceIdList 资源id列表
     * @param pathArray 路径列表
     */
    private void deletePath(List<Long> resourceIdList, JSONArray pathArray) {
        for (Long resourceId : resourceIdList) {
            List<InspectResourceConfigurationFilePathVo> inspectResourceConfigurationFilePathList = inspectConfigurationFileMapper.getInpectResourceConfigurationFilePathListByResourceId(resourceId);
            if (CollectionUtils.isEmpty(inspectResourceConfigurationFilePathList)) {
                continue;
            }
            Map<String, Long> idMap = inspectResourceConfigurationFilePathList.stream().collect(Collectors.toMap(e -> e.getPath(), e -> e.getId()));
            List<String> oldPathList = inspectResourceConfigurationFilePathList.stream().map(InspectResourceConfigurationFilePathVo::getPath).collect(Collectors.toList());
            List<String> pathList = pathArray.toJavaList(String.class);
            List<String> needDeletePathList = ListUtils.retainAll(pathList, oldPathList);
            for (String path : needDeletePathList) {
                Long id = idMap.get(path);
                if (id != null) {
                    inspectConfigurationFileMapper.deleteResourceConfigFilePathById(id);
                }
            }
        }
    }
}

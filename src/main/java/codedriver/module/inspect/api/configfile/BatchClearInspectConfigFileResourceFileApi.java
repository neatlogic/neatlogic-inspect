/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api.configfile;

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
import codedriver.framework.inspect.dto.InspectConfigFilePathVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.inspect.dao.mapper.InspectConfigFileMapper;
import codedriver.module.inspect.service.InspectConfigFileService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class BatchClearInspectConfigFileResourceFileApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigFileMapper inspectConfigFileMapper;
    @Resource
    private InspectMapper inspectMapper;
    @Resource
    private InspectConfigFileService inspectConfigFileService;

    @Override
    public String getToken() {
        return "inspect/configfile/resource/file/batchclear";
    }

    @Override
    public String getName() {
        return "批量删除巡检配置文件资源文件";
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
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, desc = "环境id列表"),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "应用系统id列表"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "资源类型id列表"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, desc = "标签id列表"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显的资源ID列表"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "巡检状态列表"),
            @Param(name = "inspectJobPhaseNodeStatusList", type = ApiParamType.JSONARRAY, desc = "巡检作业状态列表")
    })
    @Output({})
    @Description(desc = "批量删除巡检配置文件资源文件")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        ResourceSearchVo searchVo = JSONObject.toJavaObject(paramObj, ResourceSearchVo.class);
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> resourceIdList = defaultValue.toJavaList(Long.class);
            ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
            List<CiEntityVo> ciEntityList = ciEntityCrossoverMapper.getCiEntityBaseInfoByIdList(resourceIdList);
            resourceIdList = ciEntityList.stream().map(CiEntityVo::getId).collect(Collectors.toList());
            List<InspectConfigFilePathVo> inspectResourceConfigFilePathList = inspectConfigFileMapper.getInspectConfigFilePathListByResourceIdList(resourceIdList);
            inspectConfigFileService.clearFile(resourceIdList, inspectResourceConfigFilePathList);
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
                    List<InspectConfigFilePathVo> inspectResourceConfigFilePathList = inspectConfigFileMapper.getInspectConfigFilePathListByResourceIdList(resourceIdList);
                    inspectConfigFileService.clearFile(resourceIdList, inspectResourceConfigFilePathList);
                }
            }
        }
        return null;
    }

}

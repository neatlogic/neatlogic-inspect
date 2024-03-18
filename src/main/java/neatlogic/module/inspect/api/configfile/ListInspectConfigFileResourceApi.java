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
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.inspect.dto.InspectConfigFilePathVo;
import neatlogic.framework.inspect.dto.InspectResourceVo;
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
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListInspectConfigFileResourceApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigFileMapper inspectConfigFileMapper;
    @Resource
    private AutoexecJobMapper autoexecJobMapper;
    @Override
    public String getToken() {
        return "inspect/configfile/resource/list";
    }

    @Override
    public String getName() {
        return "巡检配置文件资源列表";
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
            @Param(name = "vendorIdList", type = ApiParamType.JSONARRAY, desc = "厂商id列表"),
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, desc = "环境id列表"),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "应用系统id列表"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, desc = "标签id列表"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显的资源ID列表"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "巡检状态列表"),
            @Param(name = "inspectJobPhaseNodeStatusList", type = ApiParamType.JSONARRAY, desc = "巡检作业状态列表"),
            @Param(name = "searchField", type = ApiParamType.STRING, desc = "批量搜索字段"),
            @Param(name = "batchSearchList", type = ApiParamType.JSONARRAY, desc = "批量搜索值"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = InspectResourceVo[].class, desc = "数据列表")
    })
    @Description(desc = "巡检配置文件资源列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        List<InspectResourceVo> inspectResourceList = new ArrayList<>();
        IResourceCenterResourceCrossoverService resourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
        ResourceSearchVo searchVo = resourceCrossoverService.assembleResourceSearchVo(paramObj);
        if (CollectionUtils.isNotEmpty(searchVo.getIdList())) {
            List<Long> idList = searchVo.getIdList();
            inspectResourceList = inspectConfigFileMapper.getInspectResourceListByIdList(idList);
            List<AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeList = autoexecJobMapper.getAutoexecJobNodeListByResourceIdList(idList);
            Map<Long, AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeMap = autoexecJobPhaseNodeList.stream().collect(Collectors.toMap(e -> e.getResourceId(), e -> e));
            for (InspectResourceVo inspectResourceVo : inspectResourceList) {
                Long id = inspectResourceVo.getId();
                AutoexecJobPhaseNodeVo autoexecJobPhaseNodeVo = autoexecJobPhaseNodeMap.get(id);
                if (autoexecJobPhaseNodeVo != null) {
                    inspectResourceVo.setJobPhaseNodeVo(autoexecJobPhaseNodeVo);
                }
            }
        } else {
            IResourceCenterResourceCrossoverService resourceCenterResourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
            resourceCenterResourceCrossoverService.handleBatchSearchList(searchVo);
            int count = inspectConfigFileMapper.getInspectResourceCount(searchVo);
            if (count > 0) {
                searchVo.setRowNum(count);
                List<Long> idList = inspectConfigFileMapper.getInspectResourceIdList(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    Map<Long, List<TagVo>> tagMap = resourceCenterResourceCrossoverService.getResourceTagByResourceIdList(idList);
                    List<AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeList = autoexecJobMapper.getAutoexecJobNodeListByResourceIdList(idList);
                    Map<Long, AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeMap = autoexecJobPhaseNodeList.stream().collect(Collectors.toMap(e -> e.getResourceId(), e -> e));
                    inspectResourceList = inspectConfigFileMapper.getInspectResourceListByIdList(idList);
                    List<InspectConfigFilePathVo> inspectConfigFilePathList = inspectConfigFileMapper.getInspectConfigFileLastChangeTimeListByResourceIdList(idList);
                    Map<Long, InspectConfigFilePathVo> inspectConfigFilePathMap = inspectConfigFilePathList.stream().collect(Collectors.toMap(e -> e.getResourceId(), e -> e));
                    for (InspectResourceVo inspectResourceVo : inspectResourceList) {
                        Long id = inspectResourceVo.getId();
                        InspectConfigFilePathVo inspectConfigFilePathVo = inspectConfigFilePathMap.get(id);
                        if (inspectConfigFilePathVo != null) {
                            inspectResourceVo.setLastChangeTime(inspectConfigFilePathVo.getInspectTime());
                        }
                        List<TagVo> tagList = tagMap.get(id);
                        if (CollectionUtils.isNotEmpty(tagList)) {
                            inspectResourceVo.setTagList(tagList.stream().map(TagVo::getName).collect(Collectors.toList()));
                        }
                        AutoexecJobPhaseNodeVo autoexecJobPhaseNodeVo = autoexecJobPhaseNodeMap.get(id);
                        if (autoexecJobPhaseNodeVo != null) {
                            inspectResourceVo.setJobPhaseNodeVo(autoexecJobPhaseNodeVo);
                        }
                    }
                    //排序
                    List<InspectResourceVo> resultList = new ArrayList<>();
                    for (Long id : idList) {
                        for (InspectResourceVo inspectResourceVo : inspectResourceList) {
                            if (Objects.equals(id, inspectResourceVo.getId())) {
                                resultList.add(inspectResourceVo);
                                break;
                            }
                        }
                    }
                    inspectResourceList = resultList;
                }
            }
        }
        return TableResultUtil.getResult(inspectResourceList, searchVo);
    }
}

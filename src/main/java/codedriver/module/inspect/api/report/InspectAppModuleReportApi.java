package codedriver.module.inspect.api.report;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobResourceInspectVo;
import codedriver.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dto.InspectResourceVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/2/17 11:28 上午
 */
@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class InspectAppModuleReportApi extends PrivateApiComponentBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override

    public String getName() {
        return "获取巡检应用报告列表";
    }

    @Override
    public String getToken() {
        return "inspect/appmodule/report/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "应用模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id,envId=-2时表示无配置环境"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "巡检状态列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tableList", type = ApiParamType.JSONARRAY, desc = "巡检应用报告列表")
    })
    @Description(desc = "获取巡检应用报告列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        ResourceSearchVo searchVo = paramObj.toJavaObject(ResourceSearchVo.class);
        JSONObject resourceJson = new JSONObject();
        List<InspectResourceVo> resourceVoList = new ArrayList<>();
        IResourceCenterResourceCrossoverService resourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
        JSONArray appModuleResourceList = resourceCrossoverService.getAppModuleResourceList(searchVo);
        for (int i = 0; i < appModuleResourceList.size(); i++) {
            JSONObject jsonObject = appModuleResourceList.getJSONObject(i);
            List<InspectResourceVo> voList = JSONObject.parseArray(jsonObject.getJSONArray("tbodyList").toString(),InspectResourceVo.class);
            resourceVoList.addAll(voList);
            //voList是List<InspectResourceVo>，覆盖原来的List<ResourceVo>， 以便resourceVoList的vo的循环赋值
            jsonObject.put("tbodyList", voList);
        }
        //补充巡检相关信息
        if (CollectionUtils.isNotEmpty(resourceVoList)) {
            List<AutoexecJobResourceInspectVo> jobResourceInspectVos = autoexecJobMapper.getJobResourceInspectByResourceId(resourceVoList.stream().map(InspectResourceVo::getId).collect(Collectors.toList()));
            if (CollectionUtils.isNotEmpty(jobResourceInspectVos)) {
                for (InspectResourceVo resourceVo : resourceVoList) {
                    Optional<AutoexecJobResourceInspectVo> jobResourceInspectVoOptional = jobResourceInspectVos.stream().filter(o -> Objects.equals(o.getResourceId(), resourceVo.getId())).findFirst();
                    if (jobResourceInspectVoOptional.isPresent()) {
                        AutoexecJobResourceInspectVo jobResourceInspectVo = jobResourceInspectVoOptional.get();
                        AutoexecJobPhaseNodeVo jobPhaseNodeVo = autoexecJobMapper.getJobPhaseNodeInfoByJobPhaseIdAndResourceId(jobResourceInspectVo.getPhaseId(), jobResourceInspectVo.getResourceId());
                        resourceVo.setJobPhaseNodeVo(jobPhaseNodeVo);
                    }
                }
            }
        }
        resourceJson.put("tableList", appModuleResourceList);
        return resourceJson;
    }

}

/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobResourceInspectVo;
import codedriver.framework.cmdb.crossover.IResourceListApiCrossoverService;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dto.InspectResourceVo;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class InspectResourceReportSearchApi extends PrivateApiComponentBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "获取巡检资产报告列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "模糊搜索"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id"),
            @Param(name = "protocolIdList", type = ApiParamType.JSONARRAY, desc = "协议id列表"),
            @Param(name = "stateIdList", type = ApiParamType.JSONARRAY, desc = "状态id列表"),
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, desc = "环境id列表"),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "应用系统id列表"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "资源类型id列表"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, desc = "标签id列表"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显的资源ID列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = ResourceVo[].class, desc = "数据列表")
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        IResourceListApiCrossoverService resourceListApi = CrossoverServiceFactory.getApi(IResourceListApiCrossoverService.class);
        JSONObject resourceJson = JSONObject.parseObject(JSONObject.toJSONString(resourceListApi.myDoService(paramObj)));
        List<InspectResourceVo> resourceVoList = JSONObject.parseArray(resourceJson.getString("tbodyList"), InspectResourceVo.class);
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
                resourceJson.put("tbodyList", resourceVoList);
            }
        }
        return resourceJson;
    }

    @Override
    public String getToken() {
        return "inspect/resource/report/search";
    }
}

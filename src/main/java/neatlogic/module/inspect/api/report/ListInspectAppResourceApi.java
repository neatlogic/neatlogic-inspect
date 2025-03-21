/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.inspect.api.report;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobResourceInspectVo;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.IResourceCenterDataSource;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.ResourceCenterDataSourceFactory;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListInspectAppResourceApi extends PrivateApiComponentBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override

    public String getName() {
        return "获取巡检应用报告列表";
    }

    @Override
    public String getToken() {
        return "inspect/app/resource/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "应用模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id,envId=-2时表示无配置环境"),
            @Param(name = "viewName", type = ApiParamType.LONG, desc = "视图名"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "巡检状态列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
    })
    @Output({
            @Param(name = "tableList", type = ApiParamType.JSONARRAY, desc = "巡检应用报告列表")
    })
    @Description(desc = "获取巡检应用报告列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject resourceJson = new JSONObject();
        JSONArray list = new JSONArray();
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");
        if (appSystemId == null && appModuleId == null) {
            throw new ParamNotExistsException("应用id（appSystemId）", "应用模块id（appModuleId）");
        }
        Long envId = paramObj.getLong("envId");
        Integer currentPage = paramObj.getInteger("currentPage");
        Integer pageSize = paramObj.getInteger("pageSize");
        String viewName = paramObj.getString("viewName");
        List<String> inspectStatusList = new ArrayList<>();
        JSONArray inspectStatusArray = paramObj.getJSONArray("inspectStatusList");
        if (CollectionUtils.isNotEmpty(inspectStatusArray)) {
            inspectStatusList = inspectStatusArray.toJavaList(String.class);
        }
        IResourceCenterDataSource resourceCenterDataSource = ResourceCenterDataSourceFactory.getResourceCenterDataSource();
        JSONArray tableList = resourceCenterDataSource.getAppResourceList(appSystemId, appModuleId, envId, inspectStatusList, viewName, currentPage, pageSize);
        for (int i = 0; i < tableList.size(); i++) {
            JSONObject tableObj = tableList.getJSONObject(i);
            JSONArray theadList = tableObj.getJSONArray("theadList");
            JSONObject thead = new JSONObject();
            thead.put("key", "taskStatus");
            thead.put("title", "巡检作业状态");
            theadList.add(thead);
            JSONArray tbodyList = tableObj.getJSONArray("tbodyList");
            list.addAll(tbodyList);
        }
        //补充巡检相关信息
        if (CollectionUtils.isNotEmpty(list)) {
            List<Long> idList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                JSONObject tbodyObj = list.getJSONObject(i);
                Long id = tbodyObj.getLong("id");
                idList.add(id);
            }
            List<AutoexecJobResourceInspectVo> jobResourceInspectVos = autoexecJobMapper.getJobResourceInspectByResourceId(idList);
            if (CollectionUtils.isNotEmpty(jobResourceInspectVos)) {
                for (int i = 0; i < list.size(); i++) {
                    JSONObject tbodyObj = list.getJSONObject(i);
                    Long id = tbodyObj.getLong("id");
                    Optional<AutoexecJobResourceInspectVo> jobResourceInspectVoOptional = jobResourceInspectVos.stream().filter(o -> Objects.equals(o.getResourceId(), id)).findFirst();
                    if (jobResourceInspectVoOptional.isPresent()) {
                        AutoexecJobResourceInspectVo jobResourceInspectVo = jobResourceInspectVoOptional.get();
                        AutoexecJobPhaseNodeVo jobPhaseNodeVo = autoexecJobMapper.getJobPhaseNodeInfoByJobPhaseIdAndResourceId(jobResourceInspectVo.getPhaseId(), jobResourceInspectVo.getResourceId());
                        tbodyObj.put("jobPhaseNodeVo", jobPhaseNodeVo);
                        JSONObject taskStatus = new JSONObject();
                        taskStatus.put("value", jobPhaseNodeVo.getStatus());
                        taskStatus.put("text", jobPhaseNodeVo.getStatusName());
                        tbodyObj.put("taskStatus", taskStatus);
                    }
                }
            }
        }
        resourceJson.put("tableList", tableList);
        return resourceJson;
    }

}

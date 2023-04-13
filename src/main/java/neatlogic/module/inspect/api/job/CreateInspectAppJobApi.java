/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.inspect.api.job;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.JobTriggerType;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Transactional
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class CreateInspectAppJobApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "创建应用巡检作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "组合工具ID"),
            @Param(name = "envList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "环境列表")
    })
    @Output({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业ID")
    })
    @Description(desc = "创建应用巡检作业")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystemVo = resourceCrossoverMapper.getAppSystemById(appSystemId);
        if (appSystemVo == null) {
            throw new AppSystemNotFoundException(appSystemId);
        }
        List<Long> envIdList = new ArrayList<>();
        List<Long> allAppModuleIdList = new ArrayList<>();
        JSONArray envList = paramObj.getJSONArray("envList");
        for (int i = 0; i < envList.size(); i++) {
            JSONObject envObj = envList.getJSONObject(i);
            if (MapUtils.isEmpty(envObj)) {
                continue;
            }
            Long id = envObj.getLong("id");
            if (id == null) {
                continue;
            }
            envIdList.add(id);
            JSONArray appModuleIdArray = envObj.getJSONArray("appModuleIdList");
            if (CollectionUtils.isEmpty(appModuleIdArray)) {
                continue;
            }
            List<Long> appModuleIdList = appModuleIdArray.toJavaList(Long.class);
            allAppModuleIdList.addAll(appModuleIdList);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "inspect/app/job/create";
    }
}

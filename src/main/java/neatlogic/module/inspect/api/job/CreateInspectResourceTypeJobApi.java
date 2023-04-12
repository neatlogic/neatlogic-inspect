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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.crossover.IAutoexecJobActionCrossoverService;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteNodeConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.constvalue.JobSource;
import neatlogic.framework.inspect.dao.mapper.InspectMapper;
import neatlogic.framework.inspect.exception.InspectCiCombopNotBindException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Transactional
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class CreateInspectResourceTypeJobApi extends PrivateApiComponentBase {
    @Resource
    AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    InspectMapper inspectMapper;
    @Override
    public String getName() {
        return "创建资产类型巡检作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "资产类型ID"),
            @Param(name = "filter", type = ApiParamType.JSONOBJECT, desc = "过滤条件")
    })
    @Output({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业ID")
    })
    @Description(desc = "创建资产巡检作业")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long ciId = paramObj.getLong("ciId");
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo ciVo = ciCrossoverMapper.getCiById(ciId);
        if(ciVo == null){
            throw new CiNotFoundException(ciId);
        }
        Long combopId = inspectMapper.getCombopIdByCiId(ciId);
        if(combopId == null){
            throw new InspectCiCombopNotBindException(ciVo.getLabel());
        }
        AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
        if (combopVo == null) {
            throw new AutoexecCombopNotFoundException(combopId);
        }
        AutoexecJobVo jobVo = new AutoexecJobVo();
        jobVo.setRoundCount(64);
        jobVo.setOperationId(combopId);
        jobVo.setOperationType(CombopOperationType.COMBOP.getValue());
        jobVo.setSource(JobSource.INSPECT.getValue());
        jobVo.setParam(new JSONObject());
        jobVo.setName(ciVo.getLabel() + "(" + ciVo.getName() + ")");
        jobVo.setInvokeId(ciId);
        jobVo.setRouteId(ciId.toString());
        AutoexecCombopExecuteConfigVo executeConfig = new AutoexecCombopExecuteConfigVo();
        AutoexecCombopExecuteNodeConfigVo executeNodeConfig = new AutoexecCombopExecuteNodeConfigVo();
        JSONObject filter = paramObj.getJSONObject("filter");
        if (filter == null) {
            filter = new JSONObject();
        }
        List<Long> typeIdList = new ArrayList<>();
        typeIdList.add(ciId);
        filter.put("typeIdList", typeIdList);
        executeNodeConfig.setFilter(filter);
        executeConfig.setExecuteNodeConfig(executeNodeConfig);
        jobVo.setExecuteConfig(executeConfig);
        IAutoexecJobActionCrossoverService autoexecJobActionCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobActionCrossoverService.class);
        autoexecJobActionCrossoverService.validateAndCreateJobFromCombop(jobVo);
        IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
        jobVo.setAction(JobAction.FIRE.getValue());
        jobVo.setIsFirstFire(1);
        fireAction.doService(jobVo);
        JSONObject resultObj = new JSONObject();
        resultObj.put("jobId", jobVo.getId());
        return resultObj;
    }

    @Override
    public String getToken() {
        return "inspect/resource/type/job/create";
    }
}

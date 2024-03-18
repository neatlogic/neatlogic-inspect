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
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
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
public class CreateInspectResourceEntityJobApi extends PrivateApiComponentBase {
    @Resource
    AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    InspectMapper inspectMapper;

    @Override
    public String getName() {
        return "创建资产实体巡检作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "资产实体ID")
    })
    @Output({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业ID")
    })
    @Description(desc = "创建资产巡检作业")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long resourceId = paramObj.getLong("resourceId");
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo resourceVo = resourceCrossoverMapper.getResourceById(resourceId);
        if (resourceVo == null) {
            throw new ResourceNotFoundException(resourceId);
        }
        Long ciId = resourceVo.getTypeId();
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
        jobVo.setName(resourceVo.getName());
        jobVo.setInvokeId(ciId);
        jobVo.setRouteId(ciId.toString());
        AutoexecCombopExecuteConfigVo executeConfig = new AutoexecCombopExecuteConfigVo();
        AutoexecCombopExecuteNodeConfigVo executeNodeConfig = new AutoexecCombopExecuteNodeConfigVo();
        List<AutoexecNodeVo> selectNodeList = new ArrayList<>();
        selectNodeList.add(new AutoexecNodeVo(resourceVo));
        executeNodeConfig.setSelectNodeList(selectNodeList);
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
        return "inspect/resource/entity/job/create";
    }
}

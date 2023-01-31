package neatlogic.module.inspect.api.report;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.inspect.dao.mapper.InspectScheduleMapper;
import neatlogic.framework.inspect.dto.InspectScheduleVo;
import neatlogic.framework.inspect.exception.InspectScheduleNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class InspectScheduleGetApi extends PrivateApiComponentBase {

    @Resource
    private InspectScheduleMapper inspectScheduleMapper;

    @Override
    public String getName() {
        return "获取巡检定时任务";
    }

    @Override
    public String getToken() {
        return "inspect/schedule/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "任务id", isRequired = true)})
    @Output({@Param(explode = InspectScheduleVo.class)})
    @Description(desc = "获取巡检定时任务")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        InspectScheduleVo vo = inspectScheduleMapper.getInspectScheduleById(id);
        if (vo == null) {
            throw new InspectScheduleNotFoundException(id);
        }
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo ci = ciCrossoverMapper.getCiById(vo.getCiId());
        if (ci == null) {
            throw new CiNotFoundException(vo.getCiId());
        }
        vo.setCiLabel(ci.getLabel());
        vo.setCiName(ci.getName());
        return vo;
    }
}

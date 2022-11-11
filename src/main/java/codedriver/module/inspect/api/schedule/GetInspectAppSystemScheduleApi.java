package codedriver.module.inspect.api.schedule;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiCrossoverMapper;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dao.mapper.InspectScheduleMapper;
import codedriver.framework.inspect.dto.InspectAppSystemScheduleVo;
import codedriver.framework.inspect.dto.InspectScheduleVo;
import codedriver.framework.inspect.exception.InspectScheduleNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetInspectAppSystemScheduleApi extends PrivateApiComponentBase {

    @Resource
    private InspectScheduleMapper inspectScheduleMapper;

    @Override
    public String getName() {
        return "获取应用巡检定时任务";
    }

    @Override
    public String getToken() {
        return "inspect/appsystem/schedule/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "任务id", isRequired = true)
    })
    @Output({
            @Param(explode = InspectScheduleVo.class)
    })
    @Description(desc = "获取应用巡检定时任务")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        InspectAppSystemScheduleVo vo = inspectScheduleMapper.getInspectAppSystemScheduleById(id);
        if (vo == null) {
            throw new InspectScheduleNotFoundException(id);
        }
//        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
//        CiVo ci = ciCrossoverMapper.getCiById(vo.getCiId());
//        if (ci == null) {
//            throw new CiNotFoundException(vo.getCiId());
//        }
//        vo.setCiLabel(ci.getLabel());
//        vo.setCiName(ci.getName());
        return vo;
    }
}

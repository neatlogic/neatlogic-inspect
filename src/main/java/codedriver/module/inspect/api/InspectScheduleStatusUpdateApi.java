package codedriver.module.inspect.api;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.inspect.auth.INSPECT_MODIFY;
import codedriver.framework.inspect.dao.mapper.InspectScheduleMapper;
import codedriver.framework.inspect.dto.InspectScheduleVo;
import codedriver.framework.inspect.exception.InspectScheduleNotFoundException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class InspectScheduleStatusUpdateApi extends PrivateApiComponentBase {

    @Resource
    private InspectScheduleMapper inspectScheduleMapper;

    @Override
    public String getName() {
        return "启用/禁用巡检定时任务";
    }

    @Override
    public String getToken() {
        return "inspect/schedule/status/update";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "任务id", isRequired = true),
            @Param(name = "isActive", type = ApiParamType.ENUM, rule = "0,1", desc = "是否启用", isRequired = true),
    })
    @Description(desc = "启用/禁用巡检定时任务")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        InspectScheduleVo scheduleVo = JSON.toJavaObject(paramObj, InspectScheduleVo.class);
        if (inspectScheduleMapper.getInspectScheduleById(scheduleVo.getId()) == null) {
            throw new InspectScheduleNotFoundException(scheduleVo.getId());
        }
        scheduleVo.setLcu(UserContext.get().getUserUuid());
        inspectScheduleMapper.updateInspectScheduleStatus(scheduleVo);
        if (scheduleVo.getIsActive() == 1) {
            // todo 启动任务
        } else {
            // todo 停止任务
        }
        return null;
    }
}

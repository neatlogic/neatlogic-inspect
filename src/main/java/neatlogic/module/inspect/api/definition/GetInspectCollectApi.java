package neatlogic.module.inspect.api.definition;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.inspect.exception.InspectDefinitionNotFoundEditTargetException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.service.InspectCollectService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetInspectCollectApi extends PrivateApiComponentBase {

    @Resource
    private InspectCollectService inspectCollectService;

    @Override
    public String getName() {
        return "nmiad.getinspectcollectapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "inspect/collection/get";
    }

    @Input({@Param(name = "name", type = ApiParamType.STRING, desc = "common.name")})
    @Output({
            @Param(name = "fields", type = ApiParamType.LONG, desc = "term.inspect.fields"),
            @Param(name = "thresholds", type = ApiParamType.LONG, desc = "term.inspect.thresholds"),
            @Param(name = "userVo", explode = UserVo.class, desc = "common.editor"),
            @Param(name = "lcd", type = ApiParamType.LONG, desc = "common.editdate")
    })
    @Description(desc = "nmiad.getinspectcollectapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String name = paramObj.getString("name");
        JSONObject resultObj = inspectCollectService.getCollectionByName(name);
        if (resultObj == null) {
            throw new InspectDefinitionNotFoundEditTargetException(name);
        }
        return resultObj;
    }
}

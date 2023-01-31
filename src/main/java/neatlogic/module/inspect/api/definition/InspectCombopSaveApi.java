package neatlogic.module.inspect.api.definition;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_MODIFY;
import neatlogic.framework.inspect.dao.mapper.InspectMapper;
import neatlogic.framework.inspect.dto.InspectCiCombopVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class InspectCombopSaveApi extends PrivateApiComponentBase {

    @Resource
    InspectMapper inspectMapper;

    @Override
    public String getName() {
        return "保存模型和组合工具的关系";
    }

    @Override
    public String getToken() {
        return "inspect/combop/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "inspectCiCombopList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "集合和组合工具关系列表")})
    @Description(desc = "保存巡检规则接口，用于巡检模块的巡检工具保存")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray inspectCiCombopList = paramObj.getJSONArray("inspectCiCombopList");
        List<InspectCiCombopVo> ciVoList = null;
        if (CollectionUtils.isNotEmpty(inspectCiCombopList)) {
            ciVoList = inspectCiCombopList.toJavaList(InspectCiCombopVo.class);
        }
        inspectMapper.replaceInspectCiCombopList(ciVoList);

        return null;
    }
}

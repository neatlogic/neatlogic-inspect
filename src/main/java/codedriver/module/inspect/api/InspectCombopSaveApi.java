package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.inspect.auth.INSPECT_MODIFY;
import codedriver.framework.inspect.dao.mapper.InspectMapper;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
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
            @Param(name = "cmdbCiList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "集合和组合工具关系列表")})
    @Description(desc = "保存巡检规则接口，用于巡检模块的巡检工具保存")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray cmdbCiList = paramObj.getJSONArray("cmdbCiList");
        List<CiVo> ciVoList = null;
        if (CollectionUtils.isNotEmpty(cmdbCiList)) {
            ciVoList = cmdbCiList.toJavaList(CiVo.class);
        }
        inspectMapper.replaceInspectCiCombopList(ciVoList);

        return null;
    }
}

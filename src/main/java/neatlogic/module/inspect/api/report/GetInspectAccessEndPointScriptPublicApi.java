package neatlogic.module.inspect.api.report;

import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.dao.mapper.InspectMapper;
import neatlogic.framework.inspect.dto.InspectResourceScriptVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.publicapi.PublicApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetInspectAccessEndPointScriptPublicApi extends PublicApiComponentBase {

    @Resource
    private InspectMapper inspectMapper;

    @Override
    public String getName() {
        return "根据资源id获取对应访问入口脚本信息";
    }

    @Override
    public String getToken() {
        return "inspect/accessendpoint/script/get/public";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "resourceId", isRequired = true, type = ApiParamType.LONG, desc = "资源id")
    })
    @Output({
            @Param(explode = InspectResourceScriptVo.class)
    })
    @Description(desc = "根据资源id获取对应访问入口脚本信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject returnObject = new JSONObject();
        Long resourceId = paramObj.getLong("resourceId");
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        if (resourceCrossoverMapper.checkResourceIsExists(resourceId) == 0) {
            throw new ResourceNotFoundException(resourceId);
        }
        InspectResourceScriptVo resourceScriptVo = inspectMapper.getResourceScriptByResourceId(resourceId);
        if (resourceScriptVo == null) {
            return new JSONObject();
        }
        JSONObject config = resourceScriptVo.getConfig();
        if (MapUtils.isNotEmpty(config)) {
            String type = config.getString("type");
            if (StringUtils.equals(type, "urlConfig")) {
                returnObject.put("config", config.getJSONArray("config"));
            } else if (StringUtils.equals(type, "script")) {
                returnObject.put("config", config.getJSONObject("config"));
            }
        }
        returnObject.put("scriptId", resourceScriptVo.getScriptId());
        returnObject.put("resourceId", resourceScriptVo.getResourceId());
        return returnObject;
    }
}

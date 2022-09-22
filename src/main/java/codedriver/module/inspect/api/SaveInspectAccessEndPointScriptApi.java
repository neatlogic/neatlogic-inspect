package codedriver.module.inspect.api;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.auth.INSPECT_MODIFY;
import codedriver.framework.inspect.dao.mapper.InspectMapper;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveInspectAccessEndPointScriptApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private InspectMapper inspectMapper;

    @Override
    public String getName() {
        return "保存资源脚本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "inspect/accessendpoint/script/save";
    }

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "资源id"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, desc = "拓展配置")
    })
    @Description(desc = "保存资源脚本")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long scriptId = null;
        Long resourceId = paramObj.getLong("resourceId");
        JSONObject config = paramObj.getJSONObject("config");
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        if (resourceCrossoverMapper.checkResourceIsExists(resourceId) == 0) {
            throw new ResourceNotFoundException(resourceId);
        }
        inspectMapper.deleteResourceScriptByResourceId(resourceId);
        if (StringUtils.equals(config.getString("type"), "script")) {
            scriptId = config.getLong("script");
            AutoexecScriptVo script = autoexecScriptMapper.getScriptBaseInfoById(scriptId);
            if (script == null) {
                throw new AutoexecScriptNotFoundException(scriptId);
            }
        }
        inspectMapper.insertResourceScript(resourceId, scriptId, String.valueOf(config));
        return null;
    }
}

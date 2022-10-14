package codedriver.module.inspect.api.report;

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
import codedriver.framework.inspect.exception.InspectUrlConfigIllegalException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveInspectAccessEndPointScriptApi extends PrivateApiComponentBase {

    static final Logger logger = LoggerFactory.getLogger(SaveInspectAccessEndPointScriptApi.class);

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
        JSONObject paramConfig = paramObj.getJSONObject("config");
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        if (resourceCrossoverMapper.checkResourceIsExists(resourceId) == 0) {
            throw new ResourceNotFoundException(resourceId);
        }
        inspectMapper.deleteResourceScriptByResourceId(resourceId);
        if (StringUtils.equals(paramConfig.getString("type"), "script")) {
            JSONObject configJson = paramConfig.getJSONObject("config");
            scriptId = configJson.getLong("script");
            AutoexecScriptVo script = autoexecScriptMapper.getScriptBaseInfoById(scriptId);
            if (script == null) {
                throw new AutoexecScriptNotFoundException(scriptId);
            }
        } else if (StringUtils.equals(paramConfig.getString("type"), "urlConfig")) {
            try {
                paramConfig.getJSONArray("config");
            } catch (Exception e) {
                logger.error("保存配置URL拨测的拓展配置格式不对：" + e.getMessage(), e);
                throw new InspectUrlConfigIllegalException();
            }
        }
        inspectMapper.insertResourceScript(resourceId, scriptId, String.valueOf(paramConfig));
        return null;
    }
}

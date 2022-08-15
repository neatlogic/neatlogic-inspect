/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api.configurationfile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dto.InspectResourceConfigurationFilePathVo;
import codedriver.framework.inspect.dto.InspectResourceConfigurationFileVersionVo;
import codedriver.framework.inspect.exception.InspectResourceConfigurationFilePathNotFoundException;
import codedriver.framework.inspect.exception.InspectResourceConfigurationFileVersionNotFoundException;
import codedriver.framework.lcs.BaseLineVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.inspect.dao.mapper.InspectConfigurationFileMapper;
import codedriver.module.inspect.service.InspectConfigurationFileService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetInspectConfigurationFileVersionApi extends PrivateApiComponentBase  {

    @Resource
    private InspectConfigurationFileMapper inspectConfigurationFileMapper;
    @Resource
    private InspectConfigurationFileService InspectConfigurationFileService;

    @Override
    public String getToken() {
        return "inspect/configurationfile/version/get";
    }

    @Override
    public String getName() {
        return "查询巡检配置文件内容";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "pathId", type = ApiParamType.LONG, desc = "路径id"),
            @Param(name = "versionId", type = ApiParamType.LONG, desc = "版本id")
    })
    @Output({
            @Param(explode = InspectResourceConfigurationFileVersionVo.class, desc = "文件内容")
    })
    @Description(desc = "查询巡检配置文件内容")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long pathId = paramObj.getLong("pathId");
        Long versionId = paramObj.getLong("versionId");
        if (pathId != null) {
            InspectResourceConfigurationFilePathVo pathVo = inspectConfigurationFileMapper.getInspectResourceConfigurationFilePathById(pathId);
            if (pathVo == null) {
                throw new InspectResourceConfigurationFilePathNotFoundException(pathId);
            }
            Long fileId = pathVo.getFileId();
            InspectResourceConfigurationFileVersionVo versionVo = new InspectResourceConfigurationFileVersionVo();
            List<BaseLineVo> lineList = InspectConfigurationFileService.getLineList(fileId);
            versionVo.setId(-1L);
            versionVo.setFileId(fileId);
            versionVo.setInspectTime(pathVo.getInspectTime());
            versionVo.setMd5(pathVo.getMd5());
            versionVo.setPathId(pathId);
            versionVo.setLineList(lineList);
            return versionVo;
        } else if (versionId != null) {
            InspectResourceConfigurationFileVersionVo versionVo = inspectConfigurationFileMapper.getInspectResourceConfigurationFileVersionById(versionId);
            if (versionVo == null) {
                throw new InspectResourceConfigurationFileVersionNotFoundException(versionId);
            }
            List<BaseLineVo> lineList = InspectConfigurationFileService.getLineList(versionVo.getFileId());
            versionVo.setLineList(lineList);
            return versionVo;
        } else {
            throw new ParamNotExistsException("(路径id)pathId", "(版本id)versionId");
        }
    }
}

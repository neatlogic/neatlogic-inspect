/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api.configfile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dto.InspectConfigFilePathVo;
import codedriver.framework.inspect.dto.InspectConfigFileVersionVo;
import codedriver.framework.inspect.exception.InspectConfigFilePathNotFoundException;
import codedriver.framework.inspect.exception.InspectConfigFileVersionNotFoundException;
import codedriver.framework.lcs.BaseLineVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.inspect.dao.mapper.InspectConfigFileMapper;
import codedriver.module.inspect.service.InspectConfigFileService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetInspectConfigFileVersionApi extends PrivateApiComponentBase  {

    @Resource
    private InspectConfigFileMapper inspectConfigFileMapper;
    @Resource
    private InspectConfigFileService InspectConfigFileService;

    @Override
    public String getToken() {
        return "inspect/configfile/version/get";
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
            @Param(explode = InspectConfigFileVersionVo.class, desc = "文件内容")
    })
    @Description(desc = "查询巡检配置文件内容")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long pathId = paramObj.getLong("pathId");
        Long versionId = paramObj.getLong("versionId");
        if (pathId != null) {
            InspectConfigFilePathVo pathVo = inspectConfigFileMapper.getInspectConfigFilePathById(pathId);
            if (pathVo == null) {
                throw new InspectConfigFilePathNotFoundException(pathId);
            }
            InspectConfigFileVersionVo versionVo = new InspectConfigFileVersionVo();
            versionVo.setId(-1L);
            versionVo.setInspectTime(pathVo.getInspectTime());
            versionVo.setMd5(pathVo.getMd5());
            versionVo.setPathId(pathId);
            Long fileId = pathVo.getFileId();
            if (fileId != null) {
                List<BaseLineVo> lineList = InspectConfigFileService.getLineList(fileId);
                versionVo.setFileId(fileId);
                versionVo.setLineList(lineList);
            }
            return versionVo;
        } else if (versionId != null) {
            InspectConfigFileVersionVo versionVo = inspectConfigFileMapper.getInspectConfigFileVersionById(versionId);
            if (versionVo == null) {
                throw new InspectConfigFileVersionNotFoundException(versionId);
            }
            List<BaseLineVo> lineList = InspectConfigFileService.getLineList(versionVo.getFileId());
            versionVo.setLineList(lineList);
            return versionVo;
        } else {
            throw new ParamNotExistsException("(路径id)pathId", "(版本id)versionId");
        }
    }
}

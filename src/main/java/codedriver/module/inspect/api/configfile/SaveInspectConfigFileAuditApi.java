/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api.configfile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.exception.file.FileNotFoundException;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.file.dao.mapper.FileMapper;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dto.InspectConfigFilePathVo;
import codedriver.framework.inspect.dto.InspectConfigFileAuditVo;
import codedriver.framework.inspect.dto.InspectConfigFileVersionVo;
import codedriver.framework.inspect.exception.InspectConfigFilePathNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.inspect.dao.mapper.InspectConfigFileMapper;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Service
@Transactional
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SaveInspectConfigFileAuditApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigFileMapper inspectConfigFileMapper;

    @Resource
    private FileMapper fileMapper;

    @Override
    public String getToken() {
        return "inspect/configfile/record/save";
    }

    @Override
    public String getName() {
        return "保存巡检配置文件扫描历史";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "资源id"),
            @Param(name = "pathId", type = ApiParamType.LONG, desc = "路径id"),
            @Param(name = "path", type = ApiParamType.STRING, desc = "路径"),
            @Param(name = "inspectTime", type = ApiParamType.LONG, desc = "巡检时间"),
            @Param(name = "md5", type = ApiParamType.STRING, desc = "md5"),
            @Param(name = "fileId", type = ApiParamType.LONG, desc = "文件id"),
    })
    @Output({})
    @Description(desc = "保存巡检配置文件扫描历史")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long resourceId = paramObj.getLong("resourceId");
        ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        CiEntityVo ciEntityVo = ciEntityCrossoverMapper.getCiEntityBaseInfoById(resourceId);
        if (ciEntityVo == null) {
            throw new CiEntityNotFoundException(resourceId);
        }

        Long pathId = paramObj.getLong("pathId");
        String path = paramObj.getString("path");
        if (pathId != null) {
            InspectConfigFilePathVo inspectConfigFilePathVo = inspectConfigFileMapper.getInspectConfigFilePathById(pathId);
            if (inspectConfigFilePathVo == null) {
                throw new InspectConfigFilePathNotFoundException(ciEntityVo.getName(), pathId);
            }
        } else if (StringUtils.isNotBlank(path)) {
            List<InspectConfigFilePathVo> pathList = inspectConfigFileMapper.getInspectConfigFilePathListByResourceId(resourceId);
            for (InspectConfigFilePathVo pathVo : pathList) {
                if (Objects.equals(pathVo.getPath(), path)) {
                    pathId = pathVo.getId();
                    break;
                }
            }
            if (pathId == null) {
                throw new InspectConfigFilePathNotFoundException(ciEntityVo.getName(), path);
            }
        } else {
            throw new ParamNotExistsException("路径id(pathId)", "路径(path)");
        }
        Date inspectTime = paramObj.getDate("inspectTime");
        if (inspectTime == null) {
            inspectTime = new Date();
        }
        InspectConfigFileAuditVo recordVo = new InspectConfigFileAuditVo(inspectTime, pathId);
        inspectConfigFileMapper.insertInspectConfigFileAudit(recordVo);

        Long fileId = paramObj.getLong("fileId");
        if (fileId == null) {
            return null;
        }
        FileVo fileVo = fileMapper.getFileById(fileId);
        if (fileVo == null) {
            throw new FileNotFoundException(fileId);
        }
        String md5 = paramObj.getString("md5");
        if (StringUtils.isBlank(md5)) {
            throw new ParamNotExistsException("md5");
        }
        InspectConfigFileVersionVo versionVo = new InspectConfigFileVersionVo(md5, inspectTime, fileId, recordVo.getId(), pathId);
        inspectConfigFileMapper.insertInspectConfigFileVersion(versionVo);
        InspectConfigFilePathVo pathVo = new InspectConfigFilePathVo(pathId, md5, inspectTime, fileId);
        inspectConfigFileMapper.updateInspectConfigFilePath(pathVo);
        return null;
    }
}

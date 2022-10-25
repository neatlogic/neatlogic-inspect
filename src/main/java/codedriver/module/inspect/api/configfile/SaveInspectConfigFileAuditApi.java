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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
        return "inspect/configfile/audit/save";
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
            @Param(name = "inspectTime", type = ApiParamType.LONG, isRequired = true, desc = "巡检时间"),
            @Param(name = "modifyTime", type = ApiParamType.LONG, desc = "文件修改时间"),
            @Param(name = "md5", type = ApiParamType.STRING, desc = "md5"),
            @Param(name = "fileId", type = ApiParamType.LONG, desc = "文件id"),
            @Param(name = "inspectLogTTL", type = ApiParamType.INTEGER, desc = "保留历史记录天数"),
            @Param(name = "reserveVerCount", type = ApiParamType.INTEGER, desc = "保留版本个数")
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
                InspectConfigFilePathVo pathVo = new InspectConfigFilePathVo(resourceId, path);
                inspectConfigFileMapper.insertInspectConfigFilePath(pathVo);
                pathId = pathVo.getId();
            }
        } else {
            throw new ParamNotExistsException("路径id(pathId)", "路径(path)");
        }
        Date inspectTime = paramObj.getDate("inspectTime");
        InspectConfigFileAuditVo auditVo = new InspectConfigFileAuditVo(inspectTime, pathId);
        inspectConfigFileMapper.insertInspectConfigFileAudit(auditVo);

        Long fileId = paramObj.getLong("fileId");
        if (fileId != null) {
            FileVo fileVo = fileMapper.getFileById(fileId);
            if (fileVo == null) {
                throw new FileNotFoundException(fileId);
            }
            String md5 = paramObj.getString("md5");
            if (StringUtils.isBlank(md5)) {
                throw new ParamNotExistsException("md5");
            }
            Date modifyTime = paramObj.getDate("modifyTime");
            if (modifyTime == null) {
                throw new ParamNotExistsException("modifyTime");
            }
            InspectConfigFileVersionVo versionVo = new InspectConfigFileVersionVo(md5, modifyTime, fileId, auditVo.getId(), pathId);
            inspectConfigFileMapper.insertInspectConfigFileVersion(versionVo);
            InspectConfigFilePathVo pathVo = new InspectConfigFilePathVo(pathId, md5, modifyTime, fileId);
            inspectConfigFileMapper.updateInspectConfigFilePath(pathVo);
            inspectConfigFileMapper.insertInspectConfigFileLastChangeTime(resourceId, modifyTime);
        }

        //保留历史记录天数
        Integer inspectLogTTL = paramObj.getInteger("inspectLogTTL");
        if (inspectLogTTL != null) {
            Date startInspectTime = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(inspectLogTTL));
            InspectConfigFileAuditVo deleteAuditVo = new InspectConfigFileAuditVo(startInspectTime, pathId);
            inspectConfigFileMapper.deleteInspectConfigFileAuditByPathIdAndLEInspectTime(deleteAuditVo);
        }
        //保留版本个数
        Integer reserveVerCount = paramObj.getInteger("reserveVerCount");
        if (reserveVerCount != null) {
            if (reserveVerCount == 0) {
                List<Long> pathIdList = new ArrayList<>();
                pathIdList.add(pathId);
                inspectConfigFileMapper.deleteInspectConfigFileVersionByPathIdList(pathIdList);
            } else {
                InspectConfigFileVersionVo searchVo = new InspectConfigFileVersionVo();
                searchVo.setPathId(pathId);
                searchVo.setPageSize(1);
                searchVo.setCurrentPage(reserveVerCount + 1);
                List<Long> versionIdList = inspectConfigFileMapper.getInspectConfigFileVersionIdListByPathId(searchVo);
                if (CollectionUtils.isNotEmpty(versionIdList)) {
                    searchVo.setId(versionIdList.get(0));
                    inspectConfigFileMapper.deleteInspectConfigFileVersionByPathIdAndLEId(searchVo);
                }
            }
        }
        return null;
    }
}

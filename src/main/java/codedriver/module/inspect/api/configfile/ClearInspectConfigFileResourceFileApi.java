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
import codedriver.framework.inspect.auth.INSPECT_CONFIG_FILE_MODIFY;
import codedriver.framework.inspect.dto.InspectConfigFilePathVo;
import codedriver.framework.inspect.exception.InspectConfigFilePathNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.inspect.dao.mapper.InspectConfigFileMapper;
import codedriver.module.inspect.service.InspectConfigFileService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@AuthAction(action = INSPECT_CONFIG_FILE_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ClearInspectConfigFileResourceFileApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigFileMapper inspectConfigFileMapper;

    @Resource
    private InspectConfigFileService inspectConfigFileService;

    @Override
    public String getToken() {
        return "inspect/configfile/resource/file/clear";
    }

    @Override
    public String getName() {
        return "删除巡检配置文件资源文件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "资源id"),
            @Param(name = "pathId", type = ApiParamType.LONG, desc = "路径id")
    })
    @Output({})
    @Description(desc = "删除巡检配置文件资源文件")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long resourceId = paramObj.getLong("resourceId");
        ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        CiEntityVo ciEntityVo = ciEntityCrossoverMapper.getCiEntityBaseInfoById(resourceId);
        if (ciEntityVo == null) {
            throw new CiEntityNotFoundException(resourceId);
        }
        List<Long> resourceIdList = new ArrayList<>();
        resourceIdList.add(resourceId);
        Long pathId = paramObj.getLong("pathId");
        if (pathId != null) {
            InspectConfigFilePathVo inspectConfigFilePathVo = inspectConfigFileMapper.getInspectConfigFilePathById(pathId);
            if (inspectConfigFilePathVo == null) {
                throw new InspectConfigFilePathNotFoundException(ciEntityVo.getName(), pathId);
            }
            List<InspectConfigFilePathVo> inspectResourceConfigFilePathList = new ArrayList<>();
            inspectResourceConfigFilePathList.add(inspectConfigFilePathVo);
            inspectConfigFileService.clearFile(resourceIdList, inspectResourceConfigFilePathList);
        } else {
            List<InspectConfigFilePathVo> inspectResourceConfigFilePathList = inspectConfigFileMapper.getInspectConfigFilePathListByResourceId(resourceId);
            inspectConfigFileService.clearFile(resourceIdList, inspectResourceConfigFilePathList);
        }
        return null;
    }
}

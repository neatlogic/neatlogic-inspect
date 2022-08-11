/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api.configurationfile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.crossover.IFileCrossoverService;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dto.InspectResourceConfigurationFilePathVo;
import codedriver.framework.inspect.dto.InspectResourceConfigurationFileVersionVo;
import codedriver.framework.inspect.exception.InspectResourceConfigurationFilePathNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.inspect.dao.mapper.InspectConfigurationFileMapper;
import codedriver.module.inspect.service.InspectConfigurationFileService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.javers.common.collections.Arrays;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ClearInspectConfigurationFileResourceFileApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigurationFileMapper inspectConfigurationFileMapper;

    @Resource
    private InspectConfigurationFileService inspectConfigurationFileService;

    @Override
    public String getToken() {
        return "inspect/configurationfile/resource/file/clear";
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
        IFileCrossoverService fileCrossoverService = CrossoverServiceFactory.getApi(IFileCrossoverService.class);
        Long pathId = paramObj.getLong("pathId");
        if (pathId != null) {
            InspectResourceConfigurationFilePathVo inspectResourceConfigurationFilePathVo = inspectConfigurationFileMapper.getInpectResourceConfigurationFilePathById(pathId);
            if (inspectResourceConfigurationFilePathVo == null) {
                throw new InspectResourceConfigurationFilePathNotFoundException(pathId);
            }
            List<InspectResourceConfigurationFilePathVo> inspectResourceConfigurationFilePathList = new ArrayList<>();
            inspectResourceConfigurationFilePathList.add(inspectResourceConfigurationFilePathVo);
            inspectConfigurationFileService.clearFile(resourceIdList, inspectResourceConfigurationFilePathList);
        } else {
            List<InspectResourceConfigurationFilePathVo> inspectResourceConfigurationFilePathList = inspectConfigurationFileMapper.getInpectResourceConfigurationFilePathListByResourceId(resourceId);
            inspectConfigurationFileService.clearFile(resourceIdList, inspectResourceConfigurationFilePathList);
        }
        return null;
    }
}

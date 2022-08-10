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
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dto.InspectResourceConfigurationFilePathVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.inspect.dao.mapper.InspectConfigurationFileMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SaveInspectConfigurationFileResourcePath extends PrivateApiComponentBase {

    @Resource
    private InspectConfigurationFileMapper inspectConfigurationFileMapper;

    @Override
    public String getToken() {
        return "inspect/configurationfile/resource/path/save";
    }

    @Override
    public String getName() {
        return "保存巡检配置文件资源路径";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "资源id"),
            @Param(name = "pathList", type = ApiParamType.JSONARRAY, desc = "路径列表")
    })
    @Output({})
    @Description(desc = "保存巡检配置文件资源路径")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long resourceId = paramObj.getLong("resourceId");
        ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        CiEntityVo ciEntityVo = ciEntityCrossoverMapper.getCiEntityBaseInfoById(resourceId);
        if (ciEntityVo == null) {
            throw new CiEntityNotFoundException(resourceId);
        }

        Map<String, Long> idMap = new HashMap<>();
        List<String> oldPathList = new ArrayList<>();
        List<InspectResourceConfigurationFilePathVo> inspectResourceConfigurationFilePathList = inspectConfigurationFileMapper.getInpectResourceConfigurationFilePathListByResourceId(resourceId);
        if (CollectionUtils.isNotEmpty(inspectResourceConfigurationFilePathList)) {
            oldPathList = inspectResourceConfigurationFilePathList.stream().map(InspectResourceConfigurationFilePathVo::getPath).collect(Collectors.toList());
            idMap = inspectResourceConfigurationFilePathList.stream().collect(Collectors.toMap(e -> e.getPath(), e -> e.getId()));
        }
        JSONArray pathArray = paramObj.getJSONArray("pathList");
        if (CollectionUtils.isEmpty(pathArray) && CollectionUtils.isEmpty(oldPathList)) {
            return null;
        } else if (CollectionUtils.isEmpty(pathArray) && CollectionUtils.isNotEmpty(oldPathList)) {
            inspectConfigurationFileMapper.deleteResourceConfigFilePathByResourceId(resourceId);
        }  else if (CollectionUtils.isNotEmpty(pathArray) && CollectionUtils.isEmpty(oldPathList)) {
            for (String path : pathArray.toJavaList(String.class)) {
                InspectResourceConfigurationFilePathVo pathVo = new InspectResourceConfigurationFilePathVo(resourceId, path);
                inspectConfigurationFileMapper.insertInpectResourceConfigurationFilePath(pathVo);
            }
        } else if (CollectionUtils.isNotEmpty(pathArray) && CollectionUtils.isNotEmpty(oldPathList)) {
            List<String> pathList = pathArray.toJavaList(String.class);
            List<String> needInsertPathList = ListUtils.removeAll(pathList, oldPathList);
            for (String path : needInsertPathList) {
                InspectResourceConfigurationFilePathVo pathVo = new InspectResourceConfigurationFilePathVo(resourceId, path);
                inspectConfigurationFileMapper.insertInpectResourceConfigurationFilePath(pathVo);
            }
            List<String> needDeletePathList = ListUtils.removeAll(oldPathList, pathList);
            for (String path : needDeletePathList) {
                Long id = idMap.get(path);
                if (id != null) {
                    inspectConfigurationFileMapper.deleteResourceConfigFilePathById(id);
                }
            }
        }
        return null;
    }
}

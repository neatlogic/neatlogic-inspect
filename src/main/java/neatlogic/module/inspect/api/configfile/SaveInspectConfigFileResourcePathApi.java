/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.inspect.api.configfile;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.auth.INSPECT_CONFIG_FILE_MODIFY;
import neatlogic.framework.inspect.dto.InspectConfigFilePathVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.dao.mapper.InspectConfigFileMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@AuthAction(action = INSPECT_CONFIG_FILE_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SaveInspectConfigFileResourcePathApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigFileMapper inspectConfigFileMapper;

    @Override
    public String getToken() {
        return "inspect/configfile/resource/path/save";
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
        List<InspectConfigFilePathVo> inspectResourceConfigFilePathList = inspectConfigFileMapper.getInspectConfigFilePathListByResourceId(resourceId);
        if (CollectionUtils.isNotEmpty(inspectResourceConfigFilePathList)) {
            oldPathList = inspectResourceConfigFilePathList.stream().map(InspectConfigFilePathVo::getPath).collect(Collectors.toList());
            idMap = inspectResourceConfigFilePathList.stream().collect(Collectors.toMap(e -> e.getPath(), e -> e.getId()));
        }
        JSONArray pathArray = paramObj.getJSONArray("pathList");
        if (CollectionUtils.isEmpty(pathArray) && CollectionUtils.isEmpty(oldPathList)) {
            return null;
        } else if (CollectionUtils.isEmpty(pathArray) && CollectionUtils.isNotEmpty(oldPathList)) {
            inspectConfigFileMapper.deleteInspectConfigFilePathByResourceId(resourceId);
        } else if (CollectionUtils.isNotEmpty(pathArray) && CollectionUtils.isEmpty(oldPathList)) {
            for (String path : pathArray.toJavaList(String.class)) {
                InspectConfigFilePathVo pathVo = new InspectConfigFilePathVo(resourceId, path);
                inspectConfigFileMapper.insertInspectConfigFilePath(pathVo);
            }
        } else if (CollectionUtils.isNotEmpty(pathArray) && CollectionUtils.isNotEmpty(oldPathList)) {
            List<String> pathList = pathArray.toJavaList(String.class);
            List<String> needInsertPathList = ListUtils.removeAll(pathList, oldPathList);
            for (String path : needInsertPathList) {
                InspectConfigFilePathVo pathVo = new InspectConfigFilePathVo(resourceId, path);
                inspectConfigFileMapper.insertInspectConfigFilePath(pathVo);
            }
            List<String> needDeletePathList = ListUtils.removeAll(oldPathList, pathList);
            for (String path : needDeletePathList) {
                Long id = idMap.get(path);
                if (id != null) {
                    inspectConfigFileMapper.deleteInspectConfigFilePathById(id);
                }
            }
        }
        return null;
    }
}

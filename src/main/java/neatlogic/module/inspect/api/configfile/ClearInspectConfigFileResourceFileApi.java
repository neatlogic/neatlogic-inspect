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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.auth.INSPECT_CONFIG_FILE_MODIFY;
import neatlogic.framework.inspect.dto.InspectConfigFilePathVo;
import neatlogic.framework.inspect.exception.InspectConfigFilePathNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.dao.mapper.InspectConfigFileMapper;
import neatlogic.module.inspect.service.InspectConfigFileService;
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

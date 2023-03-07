/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
 */

package neatlogic.module.inspect.api.configfile;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.inspect.dto.InspectConfigFilePathVo;
import neatlogic.framework.inspect.dto.InspectConfigFileVersionVo;
import neatlogic.framework.inspect.exception.InspectConfigFilePathNotFoundException;
import neatlogic.framework.inspect.exception.InspectConfigFileVersionNotFoundException;
import neatlogic.framework.lcs.BaseLineVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.dao.mapper.InspectConfigFileMapper;
import neatlogic.module.inspect.service.InspectConfigFileService;
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
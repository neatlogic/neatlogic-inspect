/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

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
import neatlogic.framework.lcs.BaseLineVo;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.inspect.dto.InspectConfigFileVersionVo;
import neatlogic.framework.inspect.exception.InspectConfigFilePathNotFoundException;
import neatlogic.framework.lcs.*;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.dao.mapper.InspectConfigFileMapper;
import neatlogic.module.inspect.service.InspectConfigFileService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class CompareInspectConfigFileVersionApi extends PrivateApiComponentBase {

    @Resource
    private InspectConfigFileMapper inspectConfigFileMapper;
    @Resource
    private InspectConfigFileService InspectConfigFileService;

    @Override
    public String getToken() {
        return "inspect/configfile/version/compare";
    }

    @Override
    public String getName() {
        return "对比巡检配置文件内容";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "oldVersionId", type = ApiParamType.LONG, isRequired = true, desc = "旧版本id"),
            @Param(name = "newVersionId", type = ApiParamType.LONG, isRequired = true, desc = "新版本id")
    })
    @Output({
            @Param(name = "oldVersion", explode = InspectConfigFileVersionVo.class, desc = "旧文件内容"),
            @Param(name = "newVersion", explode = InspectConfigFileVersionVo.class, desc = "新文件内容")
    })
    @Description(desc = "对比巡检配置文件内容")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long oldVersionId = paramObj.getLong("oldVersionId");
        Long newVersionId = paramObj.getLong("newVersionId");
        InspectConfigFileVersionVo oldVersionVo = inspectConfigFileMapper.getInspectConfigFileVersionById(oldVersionId);
        if (oldVersionVo == null) {
            throw new InspectConfigFilePathNotFoundException(oldVersionId);
        }
        InspectConfigFileVersionVo newVersionVo = inspectConfigFileMapper.getInspectConfigFileVersionById(newVersionId);
        if (newVersionVo == null) {
            throw new InspectConfigFilePathNotFoundException(oldVersionId);
        }
        List<BaseLineVo> oldLineList = InspectConfigFileService.getLineList(oldVersionVo.getFileId());
        List<BaseLineVo> newLineList = InspectConfigFileService.getLineList(newVersionVo.getFileId());
        List<BaseLineVo> oldResultList = new ArrayList<>();
        List<BaseLineVo> newResultList = new ArrayList<>();
        List<SegmentPair> segmentPairList = LCSUtil.LCSCompare(oldLineList, newLineList);
        for (SegmentPair segmentPair : segmentPairList) {
            LCSUtil.regroupLineList(oldLineList, newLineList, oldResultList, newResultList, segmentPair);
        }
        oldVersionVo.setLineList(oldResultList);
        newVersionVo.setLineList(newResultList);
        JSONObject resultObj = new JSONObject();
        resultObj.put("oldVersion", oldVersionVo);
        resultObj.put("newVersion", newVersionVo);
        return resultObj;
    }
}

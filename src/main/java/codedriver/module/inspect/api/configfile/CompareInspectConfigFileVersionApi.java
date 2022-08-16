/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api.configfile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.lcs.BaseLineVo;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dto.InspectConfigFileVersionVo;
import codedriver.framework.inspect.exception.InspectConfigFilePathNotFoundException;
import codedriver.framework.lcs.*;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.inspect.dao.mapper.InspectConfigFileMapper;
import codedriver.module.inspect.service.InspectConfigFileService;
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

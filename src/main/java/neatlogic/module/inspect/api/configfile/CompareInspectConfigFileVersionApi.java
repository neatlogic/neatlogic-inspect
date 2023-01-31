/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
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

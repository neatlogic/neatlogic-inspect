/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.inspect.api.report;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.constvalue.AutoexecType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author longrf
 * @date 2022/10/14 10:49
 */

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchInspectAccessEndPointScriptApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Override
    public String getName() {
        return "查询巡检脚本";
    }

    @Override
    public String getToken() {
        return "inspect/script/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显的脚本id列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecScriptVo[].class, desc = "巡检脚本列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "查询巡检脚本")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecScriptVo scriptVo = JSON.toJavaObject(paramObj, AutoexecScriptVo.class);
        scriptVo.setTypeIdList(Collections.singletonList(AutoexecType.INSPECT.getId()));
        int rowNum = autoexecScriptMapper.searchScriptCount(scriptVo);
        List<AutoexecScriptVo> returnList = new ArrayList<>();
        if (rowNum > 0) {
            scriptVo.setRowNum(rowNum);
            returnList = autoexecScriptMapper.searchScript(scriptVo);
        }
        return TableResultUtil.getResult(returnList, scriptVo);
    }
}

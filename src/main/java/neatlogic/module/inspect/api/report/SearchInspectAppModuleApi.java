/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package neatlogic.module.inspect.api.report;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

/**
 * @author longrf
 * @date 2022/10/10 17:01
 */

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchInspectAppModuleApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "查询应用巡检中应用模块列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "inspect/appmodule/search";
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊搜索"),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "应用系统id列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = ResourceVo[].class, desc = "应用巡检中应用模块列表")
    })
    @Description(desc = "查询应用巡检中应用模块列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        ResourceSearchVo searchVo = paramObj.toJavaObject(ResourceSearchVo.class);
        IResourceCenterResourceCrossoverService resourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
        return TableResultUtil.getResult(resourceCrossoverService.getAppModuleList(searchVo), searchVo);
    }
}

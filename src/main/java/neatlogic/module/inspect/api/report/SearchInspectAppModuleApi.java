/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
package neatlogic.module.inspect.api.report;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.IResourceCenterDataSource;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.ResourceCenterDataSourceFactory;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import org.springframework.stereotype.Service;

import java.util.List;

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
        IResourceCenterDataSource resourceCenterDataSource = ResourceCenterDataSourceFactory.getResourceCenterDataSource();
        List<ResourceVo> tbodyList = resourceCenterDataSource.getAppModuleList(searchVo);
        return TableResultUtil.getResult(tbodyList, searchVo);
    }
}

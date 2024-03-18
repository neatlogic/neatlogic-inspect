/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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
package neatlogic.module.inspect.api.report;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import neatlogic.framework.cmdb.dto.resourcecenter.AppEnvVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

/**
 * @author longrf
 * @date 2022/11/30 18:27
 */

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
@AuthAction(action = INSPECT_BASE.class)
public class ListInspectAppEnvApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "nmiar.listinspectappenvapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "inspect/app/env/list";
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
    })
    @Output({
            @Param(explode = AppEnvVo[].class, desc = "common.tbodylist")
    })
    @Description(desc = "nmiar.listinspectappenvapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        ResourceSearchVo searchVo = paramObj.toJavaObject(ResourceSearchVo.class);
        IResourceCenterResourceCrossoverService resourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
        return resourceCrossoverService.getAppEnvList(searchVo);
    }
}

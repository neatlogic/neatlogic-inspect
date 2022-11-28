/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.inspect.api.definition;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.auth.INSPECT_MODIFY;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.inspect.service.InspectCollectService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author longrf
 * @date 2022/11/21 17:10
 */

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetInspectResourceThresholdsSourceApi extends PrivateApiComponentBase {

    @Resource
    private InspectCollectService inspectCollectService;

    @Override
    public String getName() {
        return "获取应用巡检阈值来源";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "inspect/resource/thresholds/source/get";
    }

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "资产id")
    })
    @Output({
            @Param(name = "resourceVo", explode = ResourceVo[].class, desc = "资产信息"),
            @Param(name = "appSystemVoList", explode = ResourceVo[].class, desc = "应用系统信息列表")
    })
    @Description(desc = "获取应用巡检阈值来源，需要依赖mongodb")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject returnObj = new JSONObject();
        Long resourceId = paramObj.getLong("resourceId");
        IResourceCrossoverMapper iResourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo resourceVo = iResourceCrossoverMapper.getResourceById(resourceId);
        if (resourceVo == null) {
            throw new ResourceNotFoundException(resourceId);
        }
        returnObj.put("resourceVo", resourceVo);
        List<Long> returnAppSystemIdList = inspectCollectService.getCollectionThresholdsAppSystemIdListByResourceId(resourceId);
        if (CollectionUtils.isNotEmpty(returnAppSystemIdList)) {
            returnObj.put("appSystemVoList", iResourceCrossoverMapper.searchAppSystemListByIdList(returnAppSystemIdList));
        }
        return returnObj;
    }
}

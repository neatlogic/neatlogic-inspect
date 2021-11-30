/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.sync.CollectionVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.inspect.auth.label.INSPECT_BASE;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Service
public class InspectReportGetApi extends PrivateApiComponentBase {
    @Resource
    MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "获取巡检报告";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "resourceId", type = ApiParamType.LONG, desc = "资产id", isRequired = true)})

    @Description(desc = "根据resourceId 获取对应的巡检报告")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject inspectReport = mongoTemplate.findOne(new Query(Criteria.where("RESOURCE_ID").is(paramObj.getLong("resourceId"))), JSONObject.class, "_inspectdef");
        if (MapUtils.isNotEmpty(inspectReport)) {
            String name = inspectReport.getString("_name");
            CollectionVo collectionVo = mongoTemplate.findOne(new Query(Criteria.where("name").is(name)), CollectionVo.class, "_dictionary");
            if (collectionVo != null) {
                inspectReport.put("fields", collectionVo.getFields());
            }
        }
        return inspectReport;
    }

    @Override
    public String getToken() {
        return "inspect/report/get";
    }
}

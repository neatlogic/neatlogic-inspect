/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.sync.CollectionVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.inspect.auth.label.INSPECT_BASE;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
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

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资产id"),
            @Param(name = "id", type = ApiParamType.STRING, desc = "id")
    })

    @Description(desc = "根据resourceId 获取对应的巡检报告")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        MongoCollection<Document> collection = null;
        Long resourceId = paramObj.getLong("resourceId");
        String id = paramObj.getString("id");
        Document doc = new Document();
        //如果传resourceId则查该资产对应的最新当前报告
        if (resourceId != null) {
            collection = mongoTemplate.getDb().getCollection("INSPECT_REPORTS");
            doc.put("RESOURCE_ID", paramObj.getLong("resourceId"));
        }
        //如果传id则查对应资产的历史报告
        if (MapUtils.isEmpty(doc) && StringUtils.isNotBlank(id)) {
            collection = mongoTemplate.getDb().getCollection("INSPECT_REPORTS_HIS");
            doc.put("_id", new ObjectId(id));
        }
        if (collection == null) {
            throw new ParamIrregularException("resourceId | id");
        }

        FindIterable<Document> findIterable = collection.find(doc);
        Document reportDoc = findIterable.first();
        // TODO 通过 Query 的方式 搜不到结果集
        //JSONObject inspectReport = mongoTemplate.findOne(new Query(Criteria.where("MGMT_IP").is("192.168.0.33")), JSONObject.class, "INSPECT_REPORTS");
        //补充
        if (MapUtils.isNotEmpty(reportDoc)) {
            JSONObject reportJson = JSONObject.parseObject(reportDoc.toJson());
            JSONObject inspectResult = reportJson.getJSONObject("_inspect_result");
            if (MapUtils.isNotEmpty(inspectResult)) {
                String name = inspectResult.getString("name");
                CollectionVo collectionVo = mongoTemplate.findOne(new Query(Criteria.where("name").is(name)), CollectionVo.class, "_dictionary");
                if (collectionVo != null) {
                    reportDoc.put("fields", collectionVo.getFields());
                }
            }
        }
        return reportDoc;
    }

    @Override
    public String getToken() {
        return "inspect/report/get";
    }
}

/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.inspect.api.definition;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.inspect.auth.INSPECT_MODIFY;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * @author longrf
 * @date 2022/12/16 11:41
 */

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class ReSaveInspectCollectionThresholdsApi extends PrivateApiComponentBase {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "给巡检定义的规则增加ruleUuid（刷数据接口）";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        MongoCollection<Document> collection = mongoTemplate.getCollection("_inspectdef");
        FindIterable<Document> documents = collection.find();
        if (documents.first() != null) {
            for (Document document : documents) {
                JSONObject appDefJson = JSONObject.parseObject(document.toJson());
                JSONArray docThresholds = appDefJson.getJSONArray("thresholds");

                if (CollectionUtils.isEmpty(docThresholds)) {
                    continue;
                }
                JSONArray updateThreshold = new JSONArray();
                boolean updateFlag = false;
                for (Object object : docThresholds) {
                    JSONObject jsonObject = (JSONObject) object;
                    String ruleUuid = jsonObject.getString("ruleUuid");
                    if (StringUtils.isEmpty(ruleUuid)) {
                        ruleUuid  = UUID.randomUUID().toString().replace("-", "");
                        if (!updateFlag) {
                            updateFlag = true;
                        }
                    }
                    jsonObject.remove("ruleId");
                    jsonObject.put("ruleUuid", ruleUuid);
                    updateThreshold.add(jsonObject);
                }
                if (updateFlag) {
                    Document whereAppDoc = new Document();
                    whereAppDoc.put("name", appDefJson.getString("name"));
                    Document updateAppDoc = new Document();
                    Document setAppDoc = new Document();
                    updateAppDoc.put("thresholds", updateThreshold);
                    setAppDoc.put("$set", updateAppDoc);
                    collection.updateOne(whereAppDoc, setAppDoc);
                }
            }
        }
        return null;
    }

    @Override
    public String getToken() {
        return "inspect/collection/def/resave";
    }
}

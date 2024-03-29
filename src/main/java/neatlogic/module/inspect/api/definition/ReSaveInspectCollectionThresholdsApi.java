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
package neatlogic.module.inspect.api.definition;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.inspect.auth.INSPECT_MODIFY;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
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
                        ruleUuid = UUID.randomUUID().toString().replace("-", "");
                        if (!updateFlag) {
                            updateFlag = true;
                        }
                    }
                    JSONObject astgentime = jsonObject.getJSONObject("_astgentime");
                    if (astgentime != null) {
                        jsonObject.put("_astgentime", astgentime.getDate("$date"));

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

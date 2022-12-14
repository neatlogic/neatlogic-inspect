package codedriver.module.inspect.api.definition;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.inspect.auth.INSPECT_MODIFY;
import codedriver.framework.inspect.exception.*;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveInspectDefApi extends PrivateApiComponentBase {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "保存巡检规则";
    }

    @Override
    public String getToken() {
        return "inspect/collection/def/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "模型名称（唯一标识）"),
            @Param(name = "thresholds", type = ApiParamType.JSONARRAY, desc = "集合数据定义"),
            @Param(name = "deletedRuleUuidList", type = ApiParamType.JSONARRAY, desc = "删除的规则序号列表")
    })
    @Description(desc = "保存巡检规则接口，用于巡检模块的巡检规则保存，需要依赖mongodb")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String name = paramObj.getString("name");
        JSONArray thresholds = paramObj.getJSONArray("thresholds");
        if (!CollectionUtils.isEmpty(thresholds)) {
            List<String> nameList = new ArrayList<>();
            for (int i = 0; i < thresholds.size(); i++) {
                JSONObject thresholdTmp = thresholds.getJSONObject(i);
                if (!thresholdTmp.containsKey("name")) {
                    throw new InspectDefLessNameException(i);
                }
                if (!thresholdTmp.containsKey("level")) {
                    throw new InspectDefLessLevelException(i);
                }
                if (!thresholdTmp.containsKey("rule")) {
                    throw new InspectDefLessRuleException(i);
                }
                if (!thresholdTmp.containsKey("ruleUuid")) {
                    throw new InspectDefLessRuleUuidException(i);
                }

                //判断name是否重复
                String nameTmp = thresholdTmp.getString("name");
                if (nameList.contains(nameTmp)) {
                    throw new InspectDefRoleNameRepeatException(nameTmp);
                }
                nameList.add(thresholdTmp.getString("name"));
            }
        }
        Document whereDoc = new Document();
        Document updateDoc = new Document();
        Document setDocument = new Document();
        whereDoc.put("name", name);
        updateDoc.put("thresholds", thresholds);
        updateDoc.put("lcu", UserContext.get().getUserUuid());
        updateDoc.put("lcd", new Date());
        setDocument.put("$set", updateDoc);
        mongoTemplate.getCollection("_inspectdef").updateOne(whereDoc, setDocument);

        /*
         1、找出此模型并且含有重写规则的个性化阈值列表
         2、删除rule
         */
        JSONArray deletedRuleUuidArray = paramObj.getJSONArray("deletedRuleUuidList");
        if (CollectionUtils.isNotEmpty(deletedRuleUuidArray)) {

            //1、找出此模型并且含有重写规则的个性化阈值列表
            Document returnDoc = new Document();
            Document searchDoc = new Document();
            searchDoc.put("name", paramObj.getString("name"));
            searchDoc.put("isOverWrite", 1);
            returnDoc.put("thresholds", true);
            returnDoc.put("appSystemId", true);
            MongoCollection<Document> defAppCollection = mongoTemplate.getDb().getCollection("_inspectdef_app");
            FindIterable<Document> documents = defAppCollection.find(searchDoc).projection(returnDoc);

            //2、删除rule
            if (documents.first() != null) {
                if (CollectionUtils.isNotEmpty(deletedRuleUuidArray)) {
                    for (Document document : documents) {
                        JSONObject appDefJson = JSONObject.parseObject(document.toJson());
                        Long appSystemId = appDefJson.getLong("appSystemId");
                        JSONArray docThresholds = appDefJson.getJSONArray("thresholds");
                        if (Objects.isNull(appSystemId) || CollectionUtils.isEmpty(docThresholds)) {
                            continue;
                        }
                        //重写的规则条数
                        boolean overwriteFlag = false;
                        //需要删除的规则
                        JSONArray removeList = new JSONArray();
                        for (Object object : docThresholds) {
                            JSONObject jsonObject = (JSONObject) object;
                            String ruleUuid = jsonObject.getString("ruleUuid");
                            if (StringUtils.isEmpty(ruleUuid)) {
                                continue;
                            }
                            if (deletedRuleUuidArray.contains(ruleUuid)) {
                                removeList.add(object);
                            } else if (jsonObject.containsKey("isOverWrite") && jsonObject.getInteger("isOverWrite") == 1 && !overwriteFlag) {
                                overwriteFlag = true;
                            }
                        }
                        //重写的规则条数为0表示：没有重写的规则了
                        if (!overwriteFlag) {
                            appDefJson.put("isOverWrite", 0);
                        }
                        if (CollectionUtils.isNotEmpty(removeList)) {
                            docThresholds.removeAll(removeList);
                            Document whereAppDoc = new Document();
                            whereAppDoc.put("name", name);
                            whereAppDoc.put("appSystemId", appSystemId);
                            Document updateAppDoc = new Document();
                            Document setAppDoc = new Document();
                            updateAppDoc.put("thresholds", docThresholds);
                            if (appDefJson.containsKey("isOverWrite")) {
                                updateAppDoc.put("isOverWrite", appDefJson.getInteger("isOverWrite"));
                            }
                            setAppDoc.put("$set", updateAppDoc);
                            defAppCollection.updateOne(whereAppDoc, setAppDoc);
                        }
                    }
                }
            }
        }
        return null;
    }
}

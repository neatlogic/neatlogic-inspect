/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.inspect.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author longrf
 * @date 2022/10/24 10:45
 */

@Service
public class InspectCollectServiceImpl implements InspectCollectService {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public JSONObject getCollectionByName(String name) {
        JSONObject returnObject = new JSONObject();
        JSONArray returnFieldsArray = new JSONArray();

        //获取dictionary的数据结构（fields）
        JSONObject collectionObj = mongoTemplate.findOne(new Query(Criteria.where("name").is(name)), JSONObject.class, "_dictionary");
        if (collectionObj == null) {
            return null;
        }
        returnObject.put("label", collectionObj.getString("label"));
        returnObject.put("name", collectionObj.getString("name"));

        JSONArray dictionaryArray = collectionObj.getJSONArray("fields");

        //获取inspectDef 的指标过滤（fields）和告警规则（thresholds）
        Document doc = new Document();
        Document fieldDocument = new Document();
        doc.put("name", name);
        fieldDocument.put("fields", true);
        fieldDocument.put("thresholds", true);
        JSONObject inspectDefJson = JSONObject.parseObject(mongoTemplate.getDb().getCollection("_inspectdef").find(doc).projection(fieldDocument).first().toJson());
        JSONArray fieldsSelectedArray = inspectDefJson.getJSONArray("fields");

        //指标过滤Map
        Map<String, Integer> fieldsSelectMap = new HashMap<>();
        for (Object object : fieldsSelectedArray) {
            JSONObject dbObject = (JSONObject) JSON.toJSON(object);
            fieldsSelectMap.put(dbObject.getString("name"), dbObject.getInteger("selected"));
        }

        //根据指标过滤数据结构返回给前端
        for (Object object : dictionaryArray) {
            JSONObject dbObject = (JSONObject) JSON.toJSON(object);
            if (Objects.equals(fieldsSelectMap.get(dbObject.get("name")), 1)) {
                returnFieldsArray.add(object);
            }
        }

        returnObject.put("fields", returnFieldsArray);
        returnObject.put("thresholds", inspectDefJson.getJSONArray("thresholds"));
        return returnObject;
    }

    @Override
    public JSONArray getAllCollection() {
        JSONArray returnArray = new JSONArray();

        //所有字典集合
        List<JSONObject> allDictionaryJsonList = mongoTemplate.find(new Query(), JSONObject.class, "_dictionary");

        //所有指标过滤（fields）和告警规则（thresholds）
        Document doc = new Document();
        Document fieldDocument = new Document();
        fieldDocument.put("fields", true);
        fieldDocument.put("name", true);
        fieldDocument.put("thresholds", true);
        FindIterable<Document> allInspectDefList = mongoTemplate.getDb().getCollection("_inspectdef").find(doc).projection(fieldDocument);
        Map<String, JSONObject> allInspectNameDefJsonMap = new HashMap<>();
        for (Document document : allInspectDefList) {
            JSONObject inspectDefJson = JSONObject.parseObject(document.toJson());
            allInspectNameDefJsonMap.put(inspectDefJson.getString("name"), inspectDefJson);
        }

        for (JSONObject dictionaryJson : allDictionaryJsonList) {
            JSONObject returnObject = new JSONObject();
            JSONArray returnFieldsArray = new JSONArray();

            returnObject.put("label", dictionaryJson.getString("label"));
            returnObject.put("name", dictionaryJson.getString("name"));

            //字典数组
            JSONArray dictionaryArray = dictionaryJson.getJSONArray("fields");

            //指标过滤Map
            Map<String, Integer> fieldsSelectMap = new HashMap<>();
            JSONObject inspectDefJson = allInspectNameDefJsonMap.get(dictionaryJson.getString("name"));
            if (inspectDefJson == null) {
                continue;
            }
            for (Object object : inspectDefJson.getJSONArray("fields")) {
                JSONObject dbObject = (JSONObject) JSON.toJSON(object);
                fieldsSelectMap.put(dbObject.getString("name"), dbObject.getInteger("selected"));
            }

            //根据指标过滤数据结构
            for (Object object : dictionaryArray) {
                JSONObject dbObject = (JSONObject) JSON.toJSON(object);
                if (Objects.equals(fieldsSelectMap.get(dbObject.get("name")), 1)) {
                    returnFieldsArray.add(object);
                }
            }

            returnObject.put("fields", returnFieldsArray);
            returnObject.put("thresholds", inspectDefJson.getJSONArray("thresholds"));
            returnArray.add(returnObject);
        }

        return returnArray;
    }
}

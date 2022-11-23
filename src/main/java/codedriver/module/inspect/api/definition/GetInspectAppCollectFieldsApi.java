/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.inspect.api.definition;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.exception.sync.CollectionNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.inspect.auth.INSPECT_MODIFY;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author longrf
 * @date 2022/11/18 18:24
 */

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetInspectAppCollectFieldsApi extends PrivateApiComponentBase {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "获取应用巡检数据结构和阈值规则";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
            return "inspect/app/collection/fields/get";
    }

    @Input({
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "集合名称（唯一标识）"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用id")
    })
    @Output({
            @Param(name = "fields", type = ApiParamType.LONG, desc = "数据结构列表"),
            @Param(name = "thresholds", type = ApiParamType.LONG, desc = "阈值规则列表")
    })
    @Description(desc = "获取应用巡检数据结构和阈值规则，需要依赖mongodb")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject returnObject = new JSONObject();
        JSONArray returnFieldsArray = new JSONArray();
        Map<String, JSONObject> returnThresholdsMap = new HashMap<>();

        //获取数据结构
        JSONObject dictionary = mongoTemplate.findOne(new Query(Criteria.where("name").is(paramObj.getString("name"))), JSONObject.class, "_dictionary");
        if (dictionary == null) {
            throw new CollectionNotFoundException("_dictionary");
        }
        JSONArray dictionaryArray = dictionary.getJSONArray("fields");
        if (CollectionUtils.isEmpty(dictionaryArray)) {
            return returnObject;
        }
        //获取数据结构 fields
        Document searchDoc = new Document();
        Document fieldDocument = new Document();
        searchDoc.put("name", paramObj.getString("name"));
        fieldDocument.put("fields", true);
        fieldDocument.put("thresholds", true);
        Document defDoc = mongoTemplate.getDb().getCollection("_inspectdef").find(searchDoc).projection(fieldDocument).first();
        if (defDoc == null) {
            throw new CollectionNotFoundException("_inspectdef");
        }
        JSONArray fieldsSelectedArray = JSONObject.parseObject(defDoc.toJson()).getJSONArray("fields");
        if (CollectionUtils.isNotEmpty(fieldsSelectedArray)) {

            //1、指标过滤
            Map<String, Integer> fieldsSelectMap = new HashMap<>();
            for (Object object : fieldsSelectedArray) {
                JSONObject dbObject = (JSONObject) JSON.toJSON(object);
                fieldsSelectMap.put(dbObject.getString("name"), dbObject.getInteger("selected"));
            }
            //2、根据指标获取数据结构
            for (Object object : dictionaryArray) {
                JSONObject dbObject = (JSONObject) JSON.toJSON(object);
                if (Objects.equals(fieldsSelectMap.get(dbObject.get("name")), 1)) {
                    JSONObject dictionaryObject = new JSONObject();
                    dictionaryObject.put("name", dbObject.get("name"));
                    dictionaryObject.put("desc", dbObject.get("desc"));
                    dictionaryObject.put("type", dbObject.get("type"));
                    returnFieldsArray.add(dictionaryObject);
                }
            }
            returnObject.put("fields", returnFieldsArray);

            //获取应用个性化阈值 thresholds
            //1、获取顶层阈值规则
            JSONArray defThresholds = JSONObject.parseObject(defDoc.toJson()).getJSONArray("thresholds");
            for (Object object : defThresholds) {
                JSONObject dbObject = (JSONObject) JSON.toJSON(object);
                returnThresholdsMap.put(dbObject.getString("name"), dbObject);
            }

            //2、应用层个性化阈值覆盖
            Document returnDoc = new Document();
            searchDoc.put("appSystemId", paramObj.getLong("appSystemId"));
            returnDoc.put("thresholds", true);
            returnDoc.put("lcd", true);
            returnDoc.put("lcu", true);
            Document defAppDoc = mongoTemplate.getDb().getCollection("_inspectdef_app").find(searchDoc).projection(returnDoc).first();
            if (defAppDoc != null) {
                JSONArray defAppThresholds = JSONObject.parseObject(defAppDoc.toJson()).getJSONArray("thresholds");
                if (CollectionUtils.isNotEmpty(defAppThresholds)) {
                    for (Object object : defAppThresholds) {
                        JSONObject dbObject = (JSONObject) JSON.toJSON(object);
                        returnThresholdsMap.put(dbObject.getString("name"), dbObject);
                    }
                }
            }
            returnObject.put("thresholds", returnThresholdsMap.values());
        }
        return returnObject;
    }
}

package codedriver.module.inspect.api;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Param;
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

/**
 * @author longrf
 * @date 2022/5/24 3:38 下午
 */
@Service
public class InspectCollectionFieldsGetApi extends PrivateApiComponentBase {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "巡检定义指标过滤获取接口";
    }

    @Override
    public String getToken() {
        return "inspect/collection/fields/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "唯一标识")
    })
    @Description(desc = "巡检定义指标过滤获取接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray returnArray = new JSONArray();
        //获取数据结构
        JSONObject dictionary = mongoTemplate.findOne(new Query(Criteria.where("name").is(paramObj.getString("name"))), JSONObject.class, "_dictionary");
        JSONArray dictionaryArray = dictionary.getJSONArray("fields");
        if (CollectionUtils.isEmpty(dictionaryArray)) {
            return returnArray;
        }

        //获取过滤指标
        Document doc = new Document();
        Document fieldDocument = new Document();
        doc.put("name", paramObj.getString("name"));
        fieldDocument.put("fields",true);
        JSONArray fieldsSelectedArray = JSONObject.parseObject(mongoTemplate.getDb().getCollection("_inspectdef").find(doc).projection(fieldDocument).first().toJson()).getJSONArray("fields");
        if (CollectionUtils.isEmpty(fieldsSelectedArray)) {
            return returnArray;
        }

        //封装数据返回给前端
        Map<String, Integer> fieldsSelectMap = new HashMap<>();
        for (Object object : fieldsSelectedArray) {
            JSONObject dbObject = (JSONObject) JSON.toJSON(object);
            fieldsSelectMap.put(dbObject.getString("name"), dbObject.getInteger("selected"));
        }

        for (Object object : dictionaryArray) {
            JSONObject needObject = new JSONObject();
            JSONObject dbObject = (JSONObject) JSON.toJSON(object);
            needObject.put("name", dbObject.get("name"));
            needObject.put("desc", dbObject.get("desc"));
            needObject.put("type", dbObject.get("type"));
            needObject.put("selected", fieldsSelectMap.get(dbObject.get("name")));
            returnArray.add(needObject);
        }

        return returnArray;
    }
}

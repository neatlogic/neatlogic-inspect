package codedriver.module.inspect.api.definition;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.inspect.auth.INSPECT_MODIFY;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class InspectCollectGetApi extends PrivateApiComponentBase {


    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "获得对应的集合";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "inspect/collection/get";
    }

    @Input({@Param(name = "name", type = ApiParamType.STRING, desc = "唯一标识")})
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject returnObject = new JSONObject();
        JSONArray returnFieldsArray = new JSONArray();

        //获取dictionary的数据结构（fields）
        JSONArray dictionaryArray = mongoTemplate.findOne(new Query(Criteria.where("name").is(paramObj.getString("name"))), JSONObject.class, "_dictionary").getJSONArray("fields");

        //获取inspectdef 的指标过滤（fields）和告警规则（thresholds）
        Document doc = new Document();
        Document fieldDocument = new Document();
        doc.put("name", paramObj.getString("name"));
        fieldDocument.put("fields",true);
        fieldDocument.put("thresholds",true);
        JSONObject inspectdefJson = JSONObject.parseObject(mongoTemplate.getDb().getCollection("_inspectdef").find(doc).projection(fieldDocument).first().toJson());
        JSONArray fieldsSelectedArray = inspectdefJson.getJSONArray("fields");

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
        returnObject.put("thresholds", inspectdefJson.getJSONArray("thresholds"));
        return returnObject;
    }
}

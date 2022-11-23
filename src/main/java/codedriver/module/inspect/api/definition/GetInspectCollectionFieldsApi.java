package codedriver.module.inspect.api.definition;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.inspect.auth.INSPECT_BASE;
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

/**
 * @author longrf
 * @date 2022/5/24 3:38 下午
 */
@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetInspectCollectionFieldsApi extends PrivateApiComponentBase {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "获取巡检定义集合数据结构和阈值规则";
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
    @Output({
            @Param(name = "fields",  type = ApiParamType.LONG,desc = "数据结构列表")
    })
    @Description(desc = "获取巡检定义集合数据结构和阈值规则")
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

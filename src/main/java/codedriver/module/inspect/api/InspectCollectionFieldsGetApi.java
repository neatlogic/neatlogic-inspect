package codedriver.module.inspect.api;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
        JSONObject object = new JSONObject();
        //获取规则
        MongoCollection<Document> collection = mongoTemplate.getDb().getCollection("_inspectdef");
        Document doc = new Document();
        Document fieldDocument = new Document();
        doc.put("name", paramObj.getString("name"));
        fieldDocument.put("fields",true);
        FindIterable<Document> inspectdef = collection.find(doc).projection(fieldDocument);
        Document inspectdefDoc = inspectdef.first();
        object.put("inspectdef", inspectdefDoc);
        return object;
    }
}

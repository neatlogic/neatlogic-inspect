package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.inspect.auth.INSPECT_MODIFY;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class InspectDefGetApi extends PrivateApiComponentBase {


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
        JSONObject object = new JSONObject();
        String label = paramObj.getString("name");
        //获取数据结构
        JSONObject dictionary = mongoTemplate.findOne(new Query(Criteria.where("name").is(label)), JSONObject.class, "_dictionary");
        //获取规则
        MongoCollection<Document> collection = mongoTemplate.getDb().getCollection("_inspectdef");
        Document doc = new Document();
        doc.put("name", label);
        FindIterable<Document> inspectdef = collection.find(doc);
        Document inspectdefDoc = inspectdef.first();
        object.put("dictionary", dictionary);
        object.put("inspectdef", inspectdefDoc);
        return object;
    }

}

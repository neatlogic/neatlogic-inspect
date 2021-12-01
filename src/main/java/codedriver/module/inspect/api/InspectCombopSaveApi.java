package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.inspect.auth.label.INSPECT_MODIFY;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicUpdate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class InspectCombopSaveApi extends PrivateApiComponentBase {


    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "保存模型和组合工具的关系";
    }

    @Override
    public String getToken() {
        return "inspect/combop/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "collection", type = ApiParamType.JSONOBJECT,isRequired  = true,desc = "集合数据结构"),
            @Param(name = "collectionDef", type = ApiParamType.JSONOBJECT,isRequired  = true,desc = "集合数据定义")})
    @Description(desc = "保存巡检规则接口，用于巡检模块的巡检规则保存，需要依赖mongodb")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String name =paramObj.getString("name");
        JSONObject collection = paramObj.getJSONObject("collection");
        JSONObject collectionDef = paramObj.getJSONObject("collection");
        Update update = new BasicUpdate(collectionDef.toJSONString());

        mongoTemplate.updateMulti(new Query(Criteria.where("name").is(name)),update, "_collection_test");
        return null;
    }
}

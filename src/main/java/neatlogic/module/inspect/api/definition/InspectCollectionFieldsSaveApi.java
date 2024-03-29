package neatlogic.module.inspect.api.definition;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_MODIFY;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

/**
 * @author longrf
 * @date 2022/5/24 2:36 下午
 */
@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class InspectCollectionFieldsSaveApi extends PrivateApiComponentBase {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "保存巡检定义指标过滤接口";
    }

    @Override
    public String getToken() {
        return "inspect/collection/fields/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "模型名称（唯一标识）"),
            @Param(name = "fields", type = ApiParamType.JSONARRAY, isRequired = true,desc = "指标过滤列表")
    })
    @Description(desc = "巡检定义指标过滤保存接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Document whereDoc = new Document();
        Document updateDoc = new Document();
        Document setDocument = new Document();
        whereDoc.put("name", paramObj.getString("name"));
        updateDoc.put("fields", paramObj.getJSONArray("fields"));
        setDocument.put("$set", updateDoc);
        mongoTemplate.getCollection("_inspectdef").updateOne(whereDoc, setDocument);
        return null;
    }
}

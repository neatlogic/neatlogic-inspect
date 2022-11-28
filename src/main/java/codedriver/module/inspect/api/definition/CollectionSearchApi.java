package codedriver.module.inspect.api.definition;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.inspect.auth.INSPECT_MODIFY;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class CollectionSearchApi extends PrivateApiComponentBase {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "查询巡检模块集合列表";
    }

    @Override
    public String getToken() {
        return "inspect/collection/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字（name、label）")})
    @Description(desc = "查询巡检模块集合列表接口，用于巡检模块的巡检定义的查询，需要依赖mongodb")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String keyword = paramObj.getString("keyword");
        JSONObject result = new JSONObject();
        MongoCollection<Document> collection = mongoTemplate.getCollection("_inspectdef");
        Document orDoc = new Document();
        if (StringUtils.isNotBlank(keyword)) {
            Pattern pattern = Pattern.compile("^.*" + keyword + ".*$", Pattern.CASE_INSENSITIVE);
            Document nameDoc = new Document();
            nameDoc.put("name", pattern);
            Document labelDoc = new Document();
            labelDoc.put("label", pattern);
            orDoc.put("$or", Arrays.asList(nameDoc, labelDoc));
        }
        FindIterable<Document> collectionList = collection.find(orDoc);
        result.put("tbodyList", collectionList.into(new ArrayList<>()));
        return result;

    }
}

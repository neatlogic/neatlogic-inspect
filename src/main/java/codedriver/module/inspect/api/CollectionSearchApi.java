package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.inspect.auth.label.INSPECT_MODIFY;
import codedriver.module.inspect.dao.mapper.InspectMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class CollectionSearchApi extends PrivateApiComponentBase {

    @Autowired
    InspectMapper inspectMapper;

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

    @Input({@Param(name = "name", type = ApiParamType.STRING, desc = "名称"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")})
    @Description(desc = "查询巡检模块集合列表接口，用于巡检模块的巡检定义的查询，需要依赖mongodb")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String name = paramObj.getString("name");
        Integer pageSize = paramObj.getInteger("pageSize");
        Integer currentPage = paramObj.getInteger("currentPage");
        Boolean needPage = paramObj.getBoolean("needPage");
        JSONObject result = new JSONObject();
        MongoCollection<Document> collection = mongoTemplate.getCollection("_inspectdef");
        Document doc = new Document();
        if (StringUtils.isNotBlank(name)) {
            doc.put("name", name);
        }
        FindIterable<Document> collectionList = collection.find(doc);
        JSONArray array = collectionList.map(o -> JSONObject.parseObject(o.toJson())).into(new JSONArray());
        int rowNum = array.size();
        FindIterable<Document> collectionListPage = collection.find(doc).sort(new Document() {{
            put("name", 1);
        }}).skip(currentPage - 1).limit(pageSize);
        array = collectionListPage.map(o -> JSONObject.parseObject(o.toJson())).into(new JSONArray());
        result.put("currentPage", currentPage);
        result.put("pageSize", pageSize);
        result.put("pageCount", PageUtil.getPageCount(rowNum, pageSize));
        result.put("rowNum", rowNum);
        result.put("objects", array);
        return result;

    }
}

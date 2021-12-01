package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.sync.CollectionVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.inspect.auth.label.INSPECT_MODIFY;
import codedriver.module.inspect.dao.mapper.InspectMapper;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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

    @Input({@Param(name = "label", type = ApiParamType.STRING, desc = "名称")})
    @Output({@Param(name = "collectionList", type = ApiParamType.JSONOBJECT, desc = "集合列表")})
    @Description(desc = "查询巡检模块集合列表接口，用于巡检模块的巡检定义的查询，需要依赖mongodb")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String label = paramObj.getString("label");
        List<CollectionVo> collectionList = null;
        if (StringUtils.isNotBlank(label)) {
            Document document = new Document();
            document.put("label", label);
            FindIterable<Document> inspectdef = mongoTemplate.getCollection("_inspectdef").find(document);
            return inspectdef;
//            collectionList = mongoTemplate.find(new Document(Criteria.where("name").is(label)), CollectionVo.class, "_inspectdef").stream().distinct().collect(Collectors.toList());
        } else {
            collectionList = mongoTemplate.find(new Query(), CollectionVo.class, "_inspectdef").stream().distinct().collect(Collectors.toList());
        }
        return collectionList;

    }
}

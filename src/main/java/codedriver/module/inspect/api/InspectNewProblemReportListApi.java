package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author longrf
 * @date 2022/3/1 2:40 下午
 */
@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class InspectNewProblemReportListApi extends PrivateApiComponentBase {

    @Resource
    MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "获取巡检最新问题报告列表";
    }

    @Override
    public String getToken() {
        return "inspect/new/problem/report/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "resourceIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "资产id列表"),
    })
    @Output({
            @Param(desc = "巡检最新问题报告列表")
    })
    @Description(desc = "获取巡检资产报告接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray returnArray = new JSONArray();
        JSONArray resourceIdArray = paramObj.getJSONArray("resourceIdList");
        if (CollectionUtils.isNotEmpty(resourceIdArray)) {
            List<Long> resourceIdList = resourceIdArray.toJavaList(Long.class);
            MongoCollection<Document> collection = mongoTemplate.getCollection("INSPECT_REPORTS");
            Document doc = new Document();
            for (Long id : resourceIdList) {
                JSONObject inspectReport = new JSONObject();
                inspectReport.put("id", id);
                doc.put("RESOURCE_ID", id);
                FindIterable<Document> findIterable = collection.find(doc);
                Document reportDoc = findIterable.first();
                if (MapUtils.isNotEmpty(reportDoc)) {
                    JSONObject reportJson = JSONObject.parseObject(reportDoc.toJson());
                    JSONObject inspectResult = reportJson.getJSONObject("_inspect_result");
                    if (inspectResult != null) {
                        inspectReport.put("inspectResult", inspectResult);
                    } else {
                        inspectReport.put("inspectResult", new JSONObject());
                    }
                } else {
                    inspectReport.put("inspectResult", new JSONObject());
                }
                returnArray.add(inspectReport);
            }
        }
        return returnArray;
    }

}

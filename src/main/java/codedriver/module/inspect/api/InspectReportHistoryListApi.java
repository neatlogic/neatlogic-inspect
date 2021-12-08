/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dto.UserVo;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.common.constvalue.InspectStatus;
import codedriver.framework.restful.annotation.Description;
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
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Service
public class InspectReportHistoryListApi extends PrivateApiComponentBase {
    @Resource
    MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "获取巡检报告";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资产id", isRequired = true),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页大小", isRequired = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页", isRequired = true)
    })

    @Description(desc = "根据resourceId 获取对应的巡检报告")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        MongoCollection<Document> collection = mongoTemplate.getDb().getCollection("INSPECT_REPORTS_HIS");
        Document doc = new Document();
        doc.put("RESOURCE_ID", paramObj.getLong("resourceId"));
        Document sortDoc = new Document();
        sortDoc.put("_report_time", -1);
        FindIterable<Document> findIterable = collection.find(doc).sort(sortDoc).skip(paramObj.getInteger("currentPage")-1).limit(paramObj.getInteger("pageSize"));
        List<Document> documentList = findIterable.into(new ArrayList<>());
        for (Document reportDoc : documentList){
            String execUserUuid = reportDoc.getString("_execuser");
            reportDoc.put("_execuser",new UserVo(execUserUuid));
            Object inspectResult = reportDoc.get("_inspect_result");
            if(inspectResult != null){
                Document inspectResultDoc = (Document)inspectResult;
                String status = inspectResultDoc.getString("status");
                reportDoc.put("status", InspectStatus.getInspectStatusJson(status));
            }
        }
        return documentList;
    }

    @Override
    public String getToken() {
        return "inspect/report/history/list";
    }
}

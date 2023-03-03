/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.inspect.api.report;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.InspectStatus;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.commons.collections4.CollectionUtils;
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
    @Output({
            @Param(name = "tbodyList",explode = Document[].class, desc = "巡检报告集合"),
    })

    @Description(desc = "根据resourceId 获取对应的巡检报告")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject returnObj = new JSONObject();
        Integer currentPage = paramObj.getInteger("currentPage");
        Integer pageSize = paramObj.getInteger("pageSize");
        MongoCollection<Document> collection = mongoTemplate.getDb().getCollection("INSPECT_REPORTS_HIS");
        Document doc = new Document();
        doc.put("RESOURCE_ID", paramObj.getLong("resourceId"));
        long rowNum = collection.countDocuments(doc);
        if (rowNum > 0) {
            int skipNum = (currentPage - 1) * pageSize;
            Document sortDoc = new Document();
            sortDoc.put("_report_time", -1);
            FindIterable<Document> findIterable = collection.find(doc).sort(sortDoc).skip(skipNum).limit(pageSize);
            List<Document> documentList = findIterable.into(new ArrayList<>());
            for (Document reportDoc : documentList) {
                String execUserUuid = reportDoc.getString("_execuser");
                reportDoc.put("_execuser", new UserVo(execUserUuid));
                Object inspectResult = reportDoc.get("_inspect_result");
                if (inspectResult != null) {
                    Document inspectResultDoc = (Document) inspectResult;
                    String status = inspectResultDoc.getString("status");
                    reportDoc.put("status", InspectStatus.getInspectStatusJson(status));
                }
            }
            returnObj.put("tbodyList", documentList);
        }else {
            returnObj.put("tbodyList", CollectionUtils.EMPTY_COLLECTION);
        }

        returnObj.put("pageSize", pageSize);
        returnObj.put("currentPage", currentPage);
        returnObj.put("rowNum", rowNum);
        returnObj.put("pageCount", PageUtil.getPageCount(Math.toIntExact(rowNum), pageSize));
        return returnObj;
    }

    @Override
    public String getToken() {
        return "inspect/report/history/list";
    }
}

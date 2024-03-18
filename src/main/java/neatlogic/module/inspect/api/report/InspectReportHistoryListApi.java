/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.inspect.api.report;

import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.InspectStatus;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
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

package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dto.InspectResourceVo;
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
 * @date 2022/2/23 3:45 下午
 */
@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class InspectAutoexecJobNodeProblemReportListApi extends PrivateApiComponentBase {

    @Resource
    MongoTemplate mongoTemplate;

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "查询巡检作业节点资产问题报告列表";
    }

    @Override
    public String getToken() {
        return "inspect/autoexec/job/node/problem/report/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "作业id"),
            @Param(name = "resourceIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "资产id列表")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = InspectResourceVo[].class, desc = "巡检作业节点资产问题报告列表")
    })
    @Description(desc = "查询巡检作业节点资产问题报告列表接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long jobId = paramObj.getLong("jobId");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        JSONArray returnArray = new JSONArray();
        JSONArray resourceIdArray = paramObj.getJSONArray("resourceIdList");
        if (CollectionUtils.isNotEmpty(resourceIdArray)) {
            List<Long> resourceIdList = resourceIdArray.toJavaList(Long.class);
            MongoCollection<Document> collection = mongoTemplate.getCollection("INSPECT_REPORTS_HIS");
            Document doc = new Document();
            doc.put("_jobid", jobId.toString());
            for (Long id : resourceIdList) {
                JSONObject inspectReport = new JSONObject();
                inspectReport.put("id", id);
                inspectReport.put("inspectResult", new JSONObject());
                doc.put("RESOURCE_ID", id);
                FindIterable<Document> findIterable = collection.find(doc);
                Document reportDoc = findIterable.first();
                if (MapUtils.isNotEmpty(reportDoc)) {
                    JSONObject reportJson = JSONObject.parseObject(reportDoc.toJson());
                    JSONObject inspectResult = reportJson.getJSONObject("_inspect_result");
                    if (inspectResult != null) {
                        inspectReport.put("inspectResult", inspectResult);
                    }
                }
                returnArray.add(inspectReport);
            }
        }
        return returnArray;
    }

}

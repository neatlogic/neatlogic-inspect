package codedriver.module.inspect.service;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.sync.CollectionVo;
import codedriver.framework.common.constvalue.InspectStatus;
import codedriver.framework.inspect.dao.mapper.InspectMapper;
import codedriver.framework.inspect.dto.InspectResourceVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class InspectReportServiceImpl implements InspectReportService {

    @Resource
    MongoTemplate mongoTemplate;

    @Resource
    InspectMapper inspectMapper;

    @Override
    public Document getInspectReport(Long resourceId, String id, Long jobId) {
        MongoCollection<Document> collection;
        Document doc = new Document();
        if (StringUtils.isNotBlank(id) || (jobId != null && resourceId != null)) {
            //场景1：用id查历史报告
            //场景2：用jobId和resourceId查历史报告
            collection = mongoTemplate.getDb().getCollection("INSPECT_REPORTS_HIS");
        } else {
            //场景3：用resourceId查最新报告
            collection = mongoTemplate.getDb().getCollection("INSPECT_REPORTS");
        }
        if (resourceId != null) {
            doc.put("RESOURCE_ID", resourceId);
        }
        if (StringUtils.isNotBlank(id)) {
            doc.put("_id", new ObjectId(id));
        }
        if (jobId != null) {
            doc.put("_jobid", jobId.toString());
        }

        FindIterable<Document> findIterable = collection.find(doc);
        Document reportDoc = findIterable.first();
        // TODO 通过 Query 的方式 搜不到结果集
        //JSONObject inspectReport = mongoTemplate.findOne(new Query(Criteria.where("MGMT_IP").is("192.168.0.33")), JSONObject.class, "INSPECT_REPORTS");
        //补充
        if (MapUtils.isNotEmpty(reportDoc)) {
            JSONObject reportJson = JSONObject.parseObject(reportDoc.toJson());
            JSONObject inspectResult = reportJson.getJSONObject("_inspect_result");
            if (MapUtils.isNotEmpty(inspectResult)) {
                String name = inspectResult.getString("name");
                CollectionVo collectionVo = mongoTemplate.findOne(new Query(Criteria.where("name").is(name)), CollectionVo.class, "_dictionary");
                if (collectionVo != null) {
                    reportDoc.put("fields", collectionVo.getFields());
                }
            }
            //补充inspectStatus
            reportDoc.put("inspectStatus", InspectStatus.getAllInspectStatusMap());
        }
        return reportDoc;
    }

    @Override
    public List<InspectResourceVo> getInspectAutoexecJobNodeList(Long jobId, ResourceSearchVo searchVo) {
        List<InspectResourceVo> inspectResourceVoList = null;
        int resourceCount = inspectMapper.getInspectAutoexecJobNodeResourceCount(searchVo, jobId, TenantContext.get().getDataDbName());
        if (resourceCount > 0) {
            searchVo.setRowNum(resourceCount);
            List<Long> resourceIdList = inspectMapper.getInspectAutoexecJobNodeResourceIdList(searchVo, jobId, TenantContext.get().getDataDbName());
            inspectResourceVoList = inspectMapper.getInspectResourceVoListByIdListAndJobId(resourceIdList, jobId, TenantContext.get().getDataDbName());
        }
        if (inspectResourceVoList == null) {
            inspectResourceVoList = new ArrayList<>();
        }
        return inspectResourceVoList;
    }

    @Override
    public List<InspectResourceVo> getInspectResourceReportList(ResourceSearchVo searchVo) {
        List<InspectResourceVo> inspectResourceVoList = null;
        int resourceCount = inspectMapper.getInspectResourceCount(searchVo);
        if (resourceCount > 0) {
            searchVo.setRowNum(resourceCount);
            List<Long> resourceIdList = inspectMapper.getInspectResourceIdList(searchVo);
            inspectResourceVoList = inspectMapper.getInspectResourceVoListByIdList(resourceIdList, TenantContext.get().getDataDbName());
        }
        if (inspectResourceVoList == null) {
            inspectResourceVoList = new ArrayList<>();
        }
        return inspectResourceVoList;
    }

    @Override
    public JSONObject getInspectDetailByResourceIdList(List<Long> resourceIdList) {
        JSONObject resourceAlert = new JSONObject();
        if (CollectionUtils.isNotEmpty(resourceIdList)) {
            MongoCollection<Document> collection = mongoTemplate.getCollection("INSPECT_REPORTS");
            List<String> nameList = new ArrayList<>();
            Map<String, String> fieldPathTextMap = new HashMap<>();
            for (Long resourceId : resourceIdList) {
                JSONArray resourceAlertArray = new JSONArray();
                JSONObject mongoInspectAlertDetail = getBatchInspectDetailByResourceId(resourceId, collection);
                JSONObject inspectResult = mongoInspectAlertDetail.getJSONObject("inspectResult");
                JSONObject reportJson = mongoInspectAlertDetail.getJSONObject("reportJson");
                //初始化fieldMap
                if (!nameList.contains(inspectResult.getString("name"))) {
                    nameList.add(inspectResult.getString("name"));
                    JSONArray fields = reportJson.getJSONArray("fields");
                    if (CollectionUtils.isNotEmpty(fields)) {
                        for (int k = 0; k < fields.size(); k++) {
                            JSONObject field = fields.getJSONObject(k);
                            fieldPathTextMap.put(field.getString("name"), field.getString("desc"));
                            if (Objects.equals("JsonArray", field.getString("type"))) {
                                getFieldPathTextMap(fieldPathTextMap, field.getString("name"), field.getJSONArray("subset"));
                            }
                        }
                    }
                }
                if (MapUtils.isNotEmpty(inspectResult)) {
                    JSONObject thresholds = inspectResult.getJSONObject("thresholds");
                    JSONArray alerts = inspectResult.getJSONArray("alerts");
                    if (CollectionUtils.isNotEmpty(alerts)) {
                        for (int j = 0; j < alerts.size(); j++) {
                            JSONObject alert = alerts.getJSONObject(j);
                            JSONObject threholdJson = thresholds.getJSONObject(alert.getString("ruleName"));
                            Map<String, Object> dataMap = new HashMap<>();
                            dataMap.put("alertLevel", threholdJson.getString("level"));
                            dataMap.put("alertTips", threholdJson.getString("name"));
                            dataMap.put("alertRule", threholdJson.getString("rule"));
                            dataMap.put("alertValue", alert.getString("fieldValue"));
                            //补充告警对象
                            dataMap.put("alertObject", getInspectAlertObject(reportJson, alert, threholdJson, fieldPathTextMap));
                            resourceAlertArray.add(dataMap);
                        }
                    }
                }
                resourceAlert.put(resourceId.toString(),resourceAlertArray);
            }
        }
        return resourceAlert;
    }

    @Override
    public JSONObject getInspectDetailByResourceId(Long resourceId) {
        MongoCollection<Document> collection = mongoTemplate.getCollection("INSPECT_REPORTS");
        return getBatchInspectDetailByResourceId(resourceId, collection);
    }

    @Override
    public JSONObject getBatchInspectDetailByResourceId(Long resourceId, MongoCollection<Document> collection) {
        JSONObject inspectReport = new JSONObject();
        if (resourceId != null) {
            Document doc = new Document();
            inspectReport.put("id", resourceId);
            inspectReport.put("inspectResult", new JSONObject());
            doc.put("RESOURCE_ID", resourceId);
            FindIterable<Document> findIterable = collection.find(doc);
            Document reportDoc = findIterable.first();
            if (MapUtils.isNotEmpty(reportDoc)) {
                JSONObject reportJson = JSONObject.parseObject(reportDoc.toJson());
                JSONObject inspectResult = reportJson.getJSONObject("_inspect_result");
                if (MapUtils.isNotEmpty(inspectResult)) {
                    String name = inspectResult.getString("name");
                    CollectionVo collectionVo = mongoTemplate.findOne(new Query(Criteria.where("name").is(name)), CollectionVo.class, "_dictionary");
                    if (collectionVo != null) {
                        reportJson.put("fields", collectionVo.getFields());
                    }
                    inspectReport.put("inspectResult", inspectResult);
                    inspectReport.put("reportJson", reportJson);
                }
            }
        }
        return inspectReport;
    }

    @Override
    public Object getInspectAlertObject(JSONObject reportJson, JSONObject alert, JSONObject threholdJson, Map<String, String> fieldPathTextMap) {
        Object alertObject;
        String namePath = threholdJson.getString("rule").split(" ")[0].substring(2);
        String[] namePaths = namePath.split("\\.");
        String jsonPath = alert.getString("jsonPath");
        StringBuilder parentPath = new StringBuilder();
        String[] subJsonPaths = jsonPath.split("\\.");
        if (subJsonPaths.length > 2) {
            StringBuilder parentNamePath = new StringBuilder(namePaths[0]);
            for (int k = 1; k < namePaths.length - 1; k++) {
                parentNamePath.append(".").append(namePaths[k]);
            }
            parentPath.append(subJsonPaths[0]);
            for (int k = 1; k < subJsonPaths.length - 1; k++) {
                parentPath.append(".").append(subJsonPaths[k]);
            }
            alertObject = JSONPath.read(reportJson.toJSONString(), parentPath.toString());
            JSONObject alertJson = JSON.parseObject(JSONObject.toJSONString(alertObject));
            JSONObject alertTextJson = new JSONObject();
            //遍历alertObject 翻译
            for (Map.Entry<String, Object> entry : alertJson.entrySet()) {
                String key = entry.getKey();
                String keyText = fieldPathTextMap.get(parentNamePath + "." + key);
                alertTextJson.put(key + (keyText == null ? StringUtils.EMPTY : "(" + keyText + ")"), entry.getValue());
            }
            alertObject = alertTextJson;
        } else {
            String keyText = fieldPathTextMap.get(namePath);
            alertObject = namePath + (keyText == null ? StringUtils.EMPTY : "(" + keyText + ")");
        }
        return alertObject;
    }

    @Override
    public void getFieldPathTextMap(Map<String, String> fieldPathTextMap, String parentPath, JSONArray subsetArray) {
        for (int i = 0; i < subsetArray.size(); i++) {
            StringBuilder parentPathBuilder = new StringBuilder(parentPath);
            JSONObject subset = subsetArray.getJSONObject(i);
            parentPathBuilder.append(".").append(subset.getString("name"));
            fieldPathTextMap.put(parentPathBuilder.toString(), subset.getString("desc"));
            if (Objects.equals("JsonArray", subset.getString("type"))) {
                getFieldPathTextMap(fieldPathTextMap, parentPathBuilder.toString(), subset.getJSONArray("subset"));
            }
        }
    }
}

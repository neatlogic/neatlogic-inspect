/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.service;

import codedriver.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import codedriver.framework.cmdb.dto.resourcecenter.BgVo;
import codedriver.framework.cmdb.dto.resourcecenter.IpVo;
import codedriver.framework.cmdb.dto.resourcecenter.OwnerVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.sync.CollectionVo;
import codedriver.framework.common.constvalue.InspectStatus;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.dao.mapper.InspectMapper;
import codedriver.framework.inspect.dto.InspectResourceScriptVo;
import codedriver.framework.inspect.dto.InspectResourceVo;
import codedriver.framework.util.TimeUtil;
import codedriver.framework.util.excel.ExcelBuilder;
import codedriver.framework.util.excel.SheetBuilder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

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
        int resourceCount = inspectMapper.getInspectAutoexecJobNodeResourceCount(searchVo, jobId);
        if (resourceCount > 0) {
            searchVo.setRowNum(resourceCount);
            if (StringUtils.isNotBlank(searchVo.getKeyword())) {
                int ipKeywordCount = inspectMapper.getInspectAutoexecJobNodeResourceCountByIpKeyword(searchVo, jobId);
                if (ipKeywordCount > 0) {
                    searchVo.setIsIpFieldSort(1);
                } else {
                    int nameKeywordCount = inspectMapper.getInspectAutoexecJobNodeResourceCountByNameKeyword(searchVo, jobId);
                    if (nameKeywordCount > 0) {
                        searchVo.setIsNameFieldSort(1);
                    }
                }
            }
            List<Long> resourceIdList = inspectMapper.getInspectAutoexecJobNodeResourceIdList(searchVo, jobId);
            inspectResourceVoList = inspectMapper.getInspectResourceListByIdListAndJobId(resourceIdList, jobId);
            //排序
            List<InspectResourceVo> resultList = new ArrayList<>();
            for (Long id : resourceIdList) {
                for (InspectResourceVo inspectResourceVo : inspectResourceVoList) {
                    if (Objects.equals(id, inspectResourceVo.getId())) {
                        resultList.add(inspectResourceVo);
                        break;
                    }
                }
            }
            inspectResourceVoList = resultList;
        }
        if (inspectResourceVoList == null) {
            inspectResourceVoList = new ArrayList<>();
        }
        return inspectResourceVoList;
    }

    @Override
    public List<InspectResourceVo> getInspectResourceReportList(ResourceSearchVo searchVo) {
        if (CollectionUtils.isEmpty(searchVo.getIdList())) {
            List<InspectResourceVo> inspectResourceVoList = null;
            int resourceCount = inspectMapper.getInspectResourceCount(searchVo);
            if (resourceCount > 0) {
                searchVo.setRowNum(resourceCount);
                if (StringUtils.isNotBlank(searchVo.getKeyword())) {
                    int ipKeywordCount = inspectMapper.getInspectResourceCountByIpKeyword(searchVo);
                    if (ipKeywordCount > 0) {
                        searchVo.setIsIpFieldSort(1);
                    } else {
                        int nameKeywordCount = inspectMapper.getInspectResourceCountByNameKeyword(searchVo);
                        if (nameKeywordCount > 0) {
                            searchVo.setIsNameFieldSort(1);
                        }
                    }
                }
                List<Long> resourceIdList = inspectMapper.getInspectResourceIdList(searchVo);
                inspectResourceVoList = inspectMapper.getInspectResourceListByIdList(resourceIdList);
                Map<Long, InspectResourceVo> inspectResourceMap = inspectResourceVoList.stream().collect(Collectors.toMap(InspectResourceVo::getId, e -> e));
                List<InspectResourceScriptVo> resourceScriptVoList = inspectMapper.getResourceScriptListByResourceIdList(resourceIdList);
                if (CollectionUtils.isNotEmpty(resourceScriptVoList)) {
                    for (InspectResourceScriptVo resourceScriptVo : resourceScriptVoList) {
                        inspectResourceMap.get(resourceScriptVo.getResourceId()).setScript(resourceScriptVo);
                    }
                }
                //排序
                List<InspectResourceVo> resultList = new ArrayList<>();
                for (Long id : resourceIdList) {
                    InspectResourceVo inspectResourceVo = inspectResourceMap.get(id);
                    if (inspectResourceVo != null) {
                        resultList.add(inspectResourceVo);
                    }
                }
                inspectResourceVoList = resultList;
            }
            if (inspectResourceVoList == null) {
                inspectResourceVoList = new ArrayList<>();
            }
            return inspectResourceVoList;
        } else {
            return inspectMapper.getInspectResourceListByIdList(searchVo.getIdList());
        }
    }

    @Override
    public JSONObject getInspectDetailByResourceIdList(List<Long> resourceIdList) {
        return getInspectDetailByResourceIdListAndDate(resourceIdList, null);
    }

    @Override
    public JSONObject getInspectDetailByResourceIdListAndDate(List<Long> resourceIdList, Date date) {
        JSONObject resourceAlert = new JSONObject();
        if (CollectionUtils.isNotEmpty(resourceIdList)) {
            MongoCollection<Document> collection = mongoTemplate.getCollection("INSPECT_REPORTS");
            List<String> nameList = new ArrayList<>();
            Map<String, String> fieldPathTextMap = new HashMap<>();
            JSONArray mongoInspectAlertDetailArray = getInspectDetailByResourceIdListAndDateFromDb(resourceIdList, collection, date);
            for (int i = 0; i < mongoInspectAlertDetailArray.size(); i++) {
                JSONArray resourceAlertArray = new JSONArray();
                JSONObject mongoInspectAlertDetail = mongoInspectAlertDetailArray.getJSONObject(i);
                JSONObject inspectResult = mongoInspectAlertDetail.getJSONObject("inspectResult");
                JSONObject reportJson = mongoInspectAlertDetail.getJSONObject("reportJson");
                //初始化fieldMap
                if (!nameList.contains(inspectResult.getString("name"))) {
                    nameList.add(inspectResult.getString("name"));
                    JSONArray fields;
                    if (MapUtils.isNotEmpty(reportJson) && CollectionUtils.isNotEmpty(fields = reportJson.getJSONArray("fields"))) {
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
                            Map<String, Object> dataMap = new HashMap<>();
                            JSONObject threholdJson;
                            if (MapUtils.isNotEmpty(thresholds) && MapUtils.isNotEmpty(threholdJson = thresholds.getJSONObject(alert.getString("ruleName")))) {
                                dataMap.put("alertLevel", threholdJson.getString("level"));
                                dataMap.put("alertTips", threholdJson.getString("name"));
                                dataMap.put("alertRule", threholdJson.getString("rule"));
                                //补充告警对象
                                dataMap.put("alertObject", getInspectAlertObject(reportJson, alert, threholdJson, fieldPathTextMap));
                                if (reportJson.containsKey("_report_time") && reportJson.get("_report_time") != null) {
                                    dataMap.put("reportTime", reportJson.getJSONObject("_report_time").getDate("$date"));
                                }
                            }
                            dataMap.put("alertValue", alert.getString("fieldValue"));

                            resourceAlertArray.add(dataMap);
                        }
                    }
                }
                resourceAlert.put(mongoInspectAlertDetail.getString("id"), resourceAlertArray);
            }
        }
        return resourceAlert;
    }

    @Override
    public JSONArray getInspectDetailByResourceIdListFromDb(List<Long> resourceIdList) {
        MongoCollection<Document> collection = mongoTemplate.getCollection("INSPECT_REPORTS");
        return getInspectDetailByResourceIdListFromDb(resourceIdList, collection);
    }

    @Override
    public JSONArray getInspectDetailByResourceIdListFromDb(List<Long> resourceIdList, MongoCollection<Document> collection) {
        return this.getInspectDetailByResourceIdListAndDateFromDb(resourceIdList, collection, null);
    }

    @Override
    public JSONArray getInspectDetailByResourceIdListAndDateFromDb(List<Long> resourceIdList, MongoCollection<Document> collection, Date date) {
        JSONArray inspectReportArray = new JSONArray();
        if (CollectionUtils.isNotEmpty(resourceIdList)) {
            Document doc = new Document();
            if (date != null) {
                Calendar startTimeCalendar = Calendar.getInstance();
                startTimeCalendar.setTime(date);
                Date startDate = startTimeCalendar.getTime();
                startTimeCalendar.add(Calendar.DAY_OF_MONTH, 1);
                Date endDate = startTimeCalendar.getTime();
                doc.put("_report_time", new Document("$gte", startDate).append("$lt", endDate));
            }
            doc.put("RESOURCE_ID", new Document("$in", resourceIdList));
            FindIterable<Document> findIterable = collection.find(doc);
            for (Document document : findIterable) {
                JSONObject inspectReport = new JSONObject();
                inspectReport.put("id", document.getLong("RESOURCE_ID"));
                inspectReport.put("inspectResult", new JSONObject());
                if (MapUtils.isNotEmpty(inspectReport)) {
                    JSONObject reportJson = JSONObject.parseObject(document.toJson());
                    JSONObject inspectResult = reportJson.getJSONObject("_inspect_result");
                    if (MapUtils.isNotEmpty(inspectResult)) {
                        String name = inspectResult.getString("name");
                        CollectionVo collectionVo = mongoTemplate.findOne(new Query(Criteria.where("name").is(name)), CollectionVo.class, "_dictionary");
                        if (collectionVo != null) {
                            reportJson.put("fields", collectionVo.getFields());
                        }
                        inspectReport.put("inspectResult", inspectResult);
                        inspectReport.put("reportJson", reportJson);
                        inspectReportArray.add(inspectReport);
                    }
                }
            }

        }
        return inspectReportArray;
    }

    @Override
    public Object getInspectAlertObject(JSONObject reportJson, JSONObject alert, JSONObject threholdJson, Map<String, String> fieldPathTextMap) {
        Object alertObject;
        StringBuilder valueSB = new StringBuilder();
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
                //alertTextJson.put(key + (keyText == null ? StringUtils.EMPTY : "(" + keyText + ")"), entry.getValue());
                valueSB.append(key).append(keyText == null ? StringUtils.EMPTY : "(" + keyText + ")").append(" = ").append(entry.getValue()).append("\r\n");
            }
            alertObject = alertTextJson;
        } else {
            String keyText = fieldPathTextMap.get(namePath);
            //alertObject = namePath + (keyText == null ? StringUtils.EMPTY : "(" + keyText + ")");
            valueSB.append(namePath).append(keyText == null ? StringUtils.EMPTY : "(" + keyText + ")").append("\r\n");
        }
        return valueSB.toString();
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

    @Override
    public Workbook getInspectNewProblemReportWorkbook(ResourceSearchVo searchVo, Integer isNeedAlertDetail) {
        IResourceCenterResourceCrossoverService resourceCenterResourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
        List<Long> typeIdList = resourceCenterResourceCrossoverService.getDownwardCiIdListByCiIdList(Arrays.asList(searchVo.getTypeId()));
        searchVo.setTypeIdList(typeIdList);
        int resourceCount = inspectMapper.getInspectResourceCount(searchVo);
        searchVo.setRowNum(resourceCount);
        if (resourceCount > 0) {
            MongoCollection<Document> collection = null;
            ExcelBuilder builder = new ExcelBuilder(SXSSFWorkbook.class);
            List<String> headerList = new ArrayList<>();
            List<String> columnList = new ArrayList<>();
            headerList.add("资产id");
            columnList.add("resourceId");
            headerList.add("IP地址");
            columnList.add("ip:port");
            headerList.add("类型");
            columnList.add("typeLabel");
            headerList.add("名称");
            columnList.add("name");
            headerList.add("描述");
            columnList.add("description");
            headerList.add("监控状态");
            columnList.add("monitorStatus");
            headerList.add("巡检状态");
            columnList.add("inspectStatus");
            headerList.add("巡检作业状态");
            columnList.add("inspectJobNodeStatus");
            headerList.add("IP列表");
            columnList.add("allIpList");
            headerList.add("所属部门");
            columnList.add("bgList");
            headerList.add("所有者");
            columnList.add("ownerList");
            headerList.add("资产状态");
            columnList.add("stateName");
            headerList.add("网络区域");
            columnList.add("networkArea");
            headerList.add("标签");
            columnList.add("tagList");
            headerList.add("维护窗口");
            columnList.add("maintenanceWindow");
            if (isNeedAlertDetail == 1) {
                headerList.add("告警对象");
                columnList.add("alertObject");
                headerList.add("告警级别");
                columnList.add("alertLevel");
                headerList.add("告警提示");
                columnList.add("alertTips");
                headerList.add("告警值");
                columnList.add("alertValue");
                headerList.add("告警规则");
                columnList.add("alertRule");
            }

            SheetBuilder sheetBuilder = builder.withBorderColor(HSSFColor.HSSFColorPredefined.GREY_40_PERCENT)
                    .withHeadFontColor(HSSFColor.HSSFColorPredefined.WHITE)
                    .withHeadBgColor(HSSFColor.HSSFColorPredefined.DARK_BLUE)
                    .withColumnWidth(30)
                    .addSheet("数据")
                    .withHeaderList(headerList)
                    .withColumnList(columnList);
            Workbook workbook = builder.build();
            List<String> nameList = new ArrayList<>();
            Map<String, String> fieldPathTextMap = new HashMap<>();
            for (int i = 1; i <= searchVo.getPageCount(); i++) {
                searchVo.setCurrentPage(i);
                List<Long> resourceIdList = inspectMapper.getInspectResourceIdList(searchVo);
                List<InspectResourceVo> inspectResourceVos = inspectMapper.getInspectResourceListByIdList(resourceIdList);
                JSONArray mongoInspectAlertDetailArray = getInspectDetailByResourceIdListFromDb(inspectResourceVos.stream().map(InspectResourceVo::getId).collect(Collectors.toList()));
                Map<Long, JSONObject> mongoInspectAlertDetailMap = mongoInspectAlertDetailArray.stream().collect(Collectors.toMap(o -> ((JSONObject) o).getLong("id"), o -> ((JSONObject) o)));
                for (InspectResourceVo inspectResourceVo : inspectResourceVos) {
                    if (isNeedAlertDetail == 1) {
                        JSONObject mongoInspectAlertDetail = mongoInspectAlertDetailMap.get(inspectResourceVo.getId());
                        if(MapUtils.isEmpty(mongoInspectAlertDetail)){
                            continue;
                        }
                        JSONObject inspectResult = mongoInspectAlertDetail.getJSONObject("inspectResult");
                        JSONObject reportJson = mongoInspectAlertDetail.getJSONObject("reportJson");
                        //初始化fieldMap
                        if (!nameList.contains(inspectResult.getString("name"))) {
                            nameList.add(inspectResult.getString("name"));
                            JSONArray fields;
                            if (MapUtils.isNotEmpty(reportJson) && CollectionUtils.isNotEmpty(fields = reportJson.getJSONArray("fields"))) {
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
                                    Map<String, Object> dataMap = new HashMap<>();
                                    putCommonDataMap(dataMap, inspectResourceVo);
                                    JSONObject threholdJson;
                                    if (MapUtils.isNotEmpty(thresholds) && MapUtils.isNotEmpty(threholdJson = thresholds.getJSONObject(alert.getString("ruleName")))) {
                                        dataMap.put("alertLevel", threholdJson.getString("level"));
                                        dataMap.put("alertTips", threholdJson.getString("name"));
                                        dataMap.put("alertRule", threholdJson.getString("rule"));
                                        //补充告警对象
                                        dataMap.put("alertObject", getInspectAlertObject(reportJson, alert, threholdJson, fieldPathTextMap));
                                    }
                                    dataMap.put("alertValue", alert.getString("fieldValue"));
                                    sheetBuilder.addData(dataMap);
                                }
                            } else {
                                Map<String, Object> dataMap = new HashMap<>();
                                putCommonDataMap(dataMap, inspectResourceVo);
                                sheetBuilder.addData(dataMap);
                            }
                        }
                    } else {
                        Map<String, Object> dataMap = new HashMap<>();
                        putCommonDataMap(dataMap, inspectResourceVo);
                        sheetBuilder.addData(dataMap);
                    }
                }
            }
            return workbook;
        }
        return null;
    }

    /**
     * 导出 put进 固定data
     *
     * @param dataMap           行数据map
     * @param inspectResourceVo 巡检资产对象
     */
    private void putCommonDataMap(Map<String, Object> dataMap, InspectResourceVo inspectResourceVo) {
        dataMap.put("ip:port", inspectResourceVo.getIp() + (inspectResourceVo.getPort() != null ? ":" + inspectResourceVo.getPort() : StringUtils.EMPTY));
        dataMap.put("typeLabel", inspectResourceVo.getTypeLabel());
        dataMap.put("resourceId", inspectResourceVo.getId());
        dataMap.put("name", inspectResourceVo.getName());
        dataMap.put("description", inspectResourceVo.getDescription());
        dataMap.put("monitorStatus", inspectResourceVo.getMonitorStatus());
        dataMap.put("inspectStatus", StringUtils.isNotBlank(inspectResourceVo.getInspectStatus()) ? InspectStatus.getText(inspectResourceVo.getInspectStatus()) + " "
                + (inspectResourceVo.getInspectTime() != null ? TimeUtil.convertDateToString(inspectResourceVo.getInspectTime(), TimeUtil.YYYY_MM_DD_HH_MM_SS)
                : StringUtils.EMPTY) : StringUtils.EMPTY);
        dataMap.put("inspectJobNodeStatus", inspectResourceVo.getJobPhaseNodeVo() != null ? inspectResourceVo.getJobPhaseNodeVo().getStatusName() : StringUtils.EMPTY);
        dataMap.put("allIpList", CollectionUtils.isNotEmpty(inspectResourceVo.getAllIp()) ? inspectResourceVo.getAllIp().stream().map(IpVo::getIp).collect(Collectors.joining(",")) : StringUtils.EMPTY);
        dataMap.put("bgList", CollectionUtils.isNotEmpty(inspectResourceVo.getBgList()) ? inspectResourceVo.getBgList().stream().map(BgVo::getBgName).collect(Collectors.joining(",")) : StringUtils.EMPTY);
        dataMap.put("ownerList", CollectionUtils.isNotEmpty(inspectResourceVo.getOwnerList()) ? inspectResourceVo.getOwnerList().stream().map(OwnerVo::getUserName).collect(Collectors.joining(",")) : StringUtils.EMPTY);
        dataMap.put("stateName", inspectResourceVo.getStateName());
        dataMap.put("networkArea", inspectResourceVo.getNetworkArea());
        dataMap.put("tagList", inspectResourceVo.getTagList());
        dataMap.put("maintenanceWindow", inspectResourceVo.getMaintenanceWindow());
    }
}

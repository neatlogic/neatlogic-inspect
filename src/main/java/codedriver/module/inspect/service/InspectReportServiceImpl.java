/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.service;

import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.cmdb.crossover.IResourceCenterCommonGenerateSqlCrossoverService;
import codedriver.framework.cmdb.crossover.IResourceCenterCustomGenerateSqlCrossoverService;
import codedriver.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import codedriver.framework.cmdb.dto.resourcecenter.*;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceInfo;
import codedriver.framework.cmdb.dto.sync.CollectionVo;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.cmdb.utils.ResourceSearchGenerateSqlUtil;
import codedriver.framework.common.constvalue.InspectStatus;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.dao.mapper.InspectMapper;
import codedriver.framework.inspect.dto.InspectResourceVo;
import codedriver.framework.inspect.dto.InspectResourceScriptVo;
import codedriver.framework.util.TableResultUtil;
import codedriver.framework.util.TimeUtil;
import codedriver.framework.util.excel.ExcelBuilder;
import codedriver.framework.util.excel.SheetBuilder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.cnfexpression.MultiOrExpression;
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
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Service
public class InspectReportServiceImpl implements InspectReportService {

    @Resource
    MongoTemplate mongoTemplate;

    @Resource
    InspectMapper inspectMapper;

    @Resource
    AutoexecJobMapper autoexecJobMapper;

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

//    @Override
//    public List<InspectResourceVo> getInspectAutoexecJobNodeList(Long jobId, ResourceSearchVo searchVo) {
//        List<InspectResourceVo> inspectResourceVoList = null;
//        int resourceCount = inspectMapper.getInspectAutoexecJobNodeResourceCount(searchVo, jobId, TenantContext.get().getDataDbName());
//        if (resourceCount > 0) {
//            searchVo.setRowNum(resourceCount);
//            List<Long> resourceIdList = inspectMapper.getInspectAutoexecJobNodeResourceIdList(searchVo, jobId, TenantContext.get().getDataDbName());
//            inspectResourceVoList = inspectMapper.getInspectResourceListByIdListAndJobId(resourceIdList, jobId, TenantContext.get().getDataDbName());
//        }
//        if (inspectResourceVoList == null) {
//            inspectResourceVoList = new ArrayList<>();
//        }
//        return inspectResourceVoList;
//    }

//    @Override
//    public List<InspectResourceVo> getInspectResourceReportList(ResourceSearchVo searchVo) {
//        if (CollectionUtils.isEmpty(searchVo.getIdList())) {
//            List<InspectResourceVo> inspectResourceVoList = null;
//            int resourceCount = inspectMapper.getInspectResourceCount(searchVo);
//            if (resourceCount > 0) {
//                searchVo.setRowNum(resourceCount);
//                List<Long> resourceIdList = inspectMapper.getInspectResourceIdList(searchVo);
//                inspectResourceVoList = inspectMapper.getInspectResourceListByIdList(resourceIdList, TenantContext.get().getDataDbName());
//                Map<Long, InspectResourceVo> inspectResourceMap = inspectResourceVoList.stream().collect(Collectors.toMap(InspectResourceVo::getId, e -> e));
//                List<InspectResourceScriptVo> resourceScriptVoList = inspectMapper.getResourceScriptListByResourceIdList(resourceIdList);
//                if (CollectionUtils.isNotEmpty(resourceScriptVoList)) {
//                    for (InspectResourceScriptVo resourceScriptVo : resourceScriptVoList) {
//                        inspectResourceMap.get(resourceScriptVo.getResourceId()).setScript(resourceScriptVo);
//                    }
//                }
//            }
//            if (inspectResourceVoList == null) {
//                inspectResourceVoList = new ArrayList<>();
//            }
//            return inspectResourceVoList;
//        } else {
//            return inspectMapper.getInspectResourceListByIdList(searchVo.getIdList(), TenantContext.get().getDataDbName());
//        }
//    }

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
                resourceAlert.put(resourceId.toString(), resourceAlertArray);
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
        IResourceCenterCommonGenerateSqlCrossoverService resourceCenterCommonGenerateSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterCommonGenerateSqlCrossoverService.class);
        IResourceCenterCustomGenerateSqlCrossoverService resourceCenterCustomGenerateSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterCustomGenerateSqlCrossoverService.class);
        List<ResourceInfo> unavailableResourceInfoList = new ArrayList<>();
        List<Long> typeIdList = resourceCenterResourceCrossoverService.getDownwardCiIdListByCiIdList(Arrays.asList(searchVo.getTypeId()));
        List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList = new ArrayList<>();
        JSONObject paramObj = (JSONObject) JSONObject.toJSON(searchVo);
        paramObj.put("typeIdList", typeIdList);
        biConsumerList.add(resourceCenterCommonGenerateSqlCrossoverService.getBiConsumerByCommonCondition(paramObj, unavailableResourceInfoList));
        biConsumerList.add(resourceCenterCustomGenerateSqlCrossoverService.getBiConsumerByProtocolIdList(paramObj, unavailableResourceInfoList));
        biConsumerList.add(resourceCenterCustomGenerateSqlCrossoverService.getBiConsumerByTagIdList(paramObj, unavailableResourceInfoList));
        biConsumerList.add(resourceCenterCustomGenerateSqlCrossoverService.getBiConsumerByKeyword(paramObj, unavailableResourceInfoList));
        biConsumerList.add(getBiConsumerByInspectJobPhaseNodeStatusList(searchVo.getInspectJobPhaseNodeStatusList()));

        PlainSelect plainSelect = resourceCenterCommonGenerateSqlCrossoverService.getResourceCountPlainSelect("", biConsumerList);
        if (plainSelect == null) {
            return null;
        }
        int resourceCount = resourceCenterCommonGenerateSqlCrossoverService.getCount(plainSelect.toString());
//        int resourceCount = inspectMapper.getInspectResourceCount(searchVo);
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
                collection = mongoTemplate.getCollection("INSPECT_REPORTS");
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
                String sql = resourceCenterCommonGenerateSqlCrossoverService.getResourceIdListSql(plainSelect);
                if (StringUtils.isBlank(sql)) {
                    continue;
                }
                List<Long> resourceIdList = resourceCenterCommonGenerateSqlCrossoverService.getIdList(sql);
//                List<Long> resourceIdList = inspectMapper.getInspectResourceIdList(searchVo);
                sql = resourceCenterCommonGenerateSqlCrossoverService.getResourceListByIdListSql(getTheadList(), resourceIdList, unavailableResourceInfoList);
                if (StringUtils.isBlank(sql)) {
                    continue;
                }
                List<ResourceVo> resourceList = resourceCenterCommonGenerateSqlCrossoverService.getResourceList(sql);
                if (CollectionUtils.isEmpty(resourceList)) {
                    continue;
                }
                List<InspectResourceVo> inspectResourceVos = convertInspectResourceList(resourceList);
//                addInspectResourceOtherInfo(inspectResourceVos);
//                List<InspectResourceVo> inspectResourceVos = inspectMapper.getInspectResourceListByIdList(resourceIdList, TenantContext.get().getDataDbName());
                for (InspectResourceVo inspectResourceVo : inspectResourceVos) {
                    if (isNeedAlertDetail == 1) {
                        JSONObject mongoInspectAlertDetail = getBatchInspectDetailByResourceId(inspectResourceVo.getId(), collection);
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
                                    putCommonDataMap(dataMap, inspectResourceVo);
                                    dataMap.put("alertLevel", threholdJson.getString("level"));
                                    dataMap.put("alertTips", threholdJson.getString("name"));
                                    dataMap.put("alertRule", threholdJson.getString("rule"));
                                    dataMap.put("alertValue", alert.getString("fieldValue"));
                                    //补充告警对象
                                    dataMap.put("alertObject", getInspectAlertObject(reportJson, alert, threholdJson, fieldPathTextMap));
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

    @Override
    public BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByInspectJobPhaseNodeStatusList(List<String> inspectJobPhaseNodeStatusList) {
        BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> biConsumer = new BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>() {
            @Override
            public void accept(ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, PlainSelect plainSelect) {
                if (CollectionUtils.isNotEmpty(inspectJobPhaseNodeStatusList)) {
                    Table mainTable = (Table) plainSelect.getFromItem();
                    Table table = new Table("autoexec_job_resource_inspect").withAlias(new Alias("e").withUseAs(false));
                    EqualsTo equalsTo = new EqualsTo()
                            .withLeftExpression(new Column(table, "resource_id"))
                            .withRightExpression(new Column(mainTable, "id"));
                    Join join = new Join().withRightItem(table).addOnExpression(equalsTo);
                    plainSelect.addJoins(join);
                    Table table2 = new Table("autoexec_job_phase_node").withAlias(new Alias("f").withUseAs(false));
                    EqualsTo equalsTo2 = new EqualsTo()
                            .withLeftExpression(new Column(table2, "resource_id"))
                            .withRightExpression(new Column(mainTable, "id"));
                    EqualsTo equalsTo3 = new EqualsTo()
                            .withLeftExpression(new Column(table2, "job_phase_id"))
                            .withRightExpression(new Column(table, "phase_id"));
                    AndExpression andExpression = new AndExpression(equalsTo2, equalsTo3);
                    InExpression inExpression = new InExpression();
                    inExpression.setLeftExpression(new Column(table2, "status"));
                    ExpressionList expressionList = new ExpressionList();
                    for (String status : inspectJobPhaseNodeStatusList) {
                        expressionList.addExpressions(new StringValue(status));
                    }
                    inExpression.setRightItemsList(expressionList);
                    Join join2 = new Join().withRightItem(table2).addOnExpression(new AndExpression(andExpression, inExpression));
                    plainSelect.addJoins(join2);
                }
            }
        };
        return biConsumer;
    }

    @Override
    public List<InspectResourceVo> convertInspectResourceList(List<ResourceVo> resourceList) {
        List<InspectResourceVo> inspectResourceVoList = new ArrayList<>();
        List<Long> idList = resourceList.stream().map(ResourceVo::getId).collect(Collectors.toList());
        IResourceCenterResourceCrossoverService resourceCenterResourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
        Map<Long, List<AccountVo>> resourceAccountVoMap = resourceCenterResourceCrossoverService.getResourceAccountByResourceIdList(idList);
        Map<Long, List<TagVo>> resourceTagVoMap = resourceCenterResourceCrossoverService.getResourceTagByResourceIdList(idList);
        List<InspectResourceScriptVo> resourceScriptVoList = inspectMapper.getResourceScriptListByResourceIdList(idList);
        Map<Long, InspectResourceScriptVo> resourceScriptMap = resourceScriptVoList.stream().collect(Collectors.toMap(e -> e.getResourceId(), e -> e));
        List<AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeList = autoexecJobMapper.getAutoexecJobNodeListByResourceIdList(idList);
        Map<Long, AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeMap = autoexecJobPhaseNodeList.stream().collect(Collectors.toMap(e -> e.getResourceId(), e -> e));
        for (ResourceVo resourceVo : resourceList) {
            InspectResourceVo inspectResourceVo = new InspectResourceVo(resourceVo);
            Long id = inspectResourceVo.getId();
            List<AccountVo> accountVoList = resourceAccountVoMap.get(id);
            if (CollectionUtils.isNotEmpty(accountVoList)) {
                inspectResourceVo.setAccountList(accountVoList);
            }
            List<TagVo> tagVoList = resourceTagVoMap.get(id);
            if (CollectionUtils.isNotEmpty(tagVoList)) {
                inspectResourceVo.setTagList(tagVoList.stream().map(TagVo::getName).collect(Collectors.toList()));
            }
            InspectResourceScriptVo inspectResourceScriptVo = resourceScriptMap.get(id);
            if (inspectResourceScriptVo != null) {
                inspectResourceVo.setScript(inspectResourceScriptVo);
            }
            AutoexecJobPhaseNodeVo autoexecJobPhaseNodeVo = autoexecJobPhaseNodeMap.get(id);
            if (autoexecJobPhaseNodeVo != null) {
                inspectResourceVo.setJobPhaseNodeVo(autoexecJobPhaseNodeVo);
            }
            inspectResourceVoList.add(inspectResourceVo);
        }
        return inspectResourceVoList;
    }

    @Override
    public List<ResourceInfo> getTheadList() {
        List<ResourceInfo> theadList = new ArrayList<>();
        theadList.add(new ResourceInfo("resource_ipobject", "id"));
        //1.IP地址:端口
        theadList.add(new ResourceInfo("resource_ipobject", "ip"));
        theadList.add(new ResourceInfo("resource_softwareservice", "port"));
        //2.类型
        theadList.add(new ResourceInfo("resource_ipobject", "type_id"));
        theadList.add(new ResourceInfo("resource_ipobject", "type_name"));
        theadList.add(new ResourceInfo("resource_ipobject", "type_label"));
        //3.名称
        theadList.add(new ResourceInfo("resource_ipobject", "name"));
        //4.监控状态
        theadList.add(new ResourceInfo("resource_ipobject", "monitor_status"));
        theadList.add(new ResourceInfo("resource_ipobject", "monitor_time"));
        //5.巡检状态
        theadList.add(new ResourceInfo("resource_ipobject", "inspect_status"));
        theadList.add(new ResourceInfo("resource_ipobject", "inspect_time"));
        //12.网络区域
        theadList.add(new ResourceInfo("resource_ipobject", "network_area"));
        //14.维护窗口
        theadList.add(new ResourceInfo("resource_ipobject", "maintenance_window"));
        //16.描述
        theadList.add(new ResourceInfo("resource_ipobject", "description"));
        //6.模块
//        theadList.add(new ResourceInfo("resource_ipobject_appmodule", "app_module_id"));
//        theadList.add(new ResourceInfo("resource_ipobject_appmodule", "app_module_name"));
//        theadList.add(new ResourceInfo("resource_ipobject_appmodule", "app_module_abbr_name"));
        //7.应用
//        theadList.add(new ResourceInfo("resource_appmodule_appsystem", "app_system_id"));
//        theadList.add(new ResourceInfo("resource_appmodule_appsystem", "app_system_name"));
//        theadList.add(new ResourceInfo("resource_appmodule_appsystem", "app_system_abbr_name"));
        //8.IP列表
        theadList.add(new ResourceInfo("resource_ipobject_allip", "allip_id"));
        theadList.add(new ResourceInfo("resource_ipobject_allip", "allip_ip"));
        theadList.add(new ResourceInfo("resource_ipobject_allip", "allip_label"));
        //9.所属部门
        theadList.add(new ResourceInfo("resource_ipobject_bg", "bg_id"));
        theadList.add(new ResourceInfo("resource_ipobject_bg", "bg_name"));
        //10.所有者
        theadList.add(new ResourceInfo("resource_ipobject_owner", "user_id"));
        theadList.add(new ResourceInfo("resource_ipobject_owner", "user_uuid"));
        theadList.add(new ResourceInfo("resource_ipobject_owner", "user_name"));
        //11.资产状态
        theadList.add(new ResourceInfo("resource_ipobject_state", "state_id"));
        theadList.add(new ResourceInfo("resource_ipobject_state", "state_name"));
        theadList.add(new ResourceInfo("resource_ipobject_state", "state_label"));
        //环境状态
//        theadList.add(new ResourceInfo("resource_softwareservice_env", "env_id"));
//        theadList.add(new ResourceInfo("resource_softwareservice_env", "env_name"));
        return theadList;
    }
}

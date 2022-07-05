/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.service;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
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

    @Override
    public List<InspectResourceVo> getInspectAutoexecJobNodeList(Long jobId, ResourceSearchVo searchVo) {
        List<InspectResourceVo> inspectResourceVoList = null;
        int resourceCount = inspectMapper.getInspectAutoexecJobNodeResourceCount(searchVo, jobId, TenantContext.get().getDataDbName());
        if (resourceCount > 0) {
            searchVo.setRowNum(resourceCount);
            List<Long> resourceIdList = inspectMapper.getInspectAutoexecJobNodeResourceIdList(searchVo, jobId, TenantContext.get().getDataDbName());
            inspectResourceVoList = inspectMapper.getInspectResourceListByIdListAndJobId(resourceIdList, jobId, TenantContext.get().getDataDbName());
        }
        if (inspectResourceVoList == null) {
            inspectResourceVoList = new ArrayList<>();
        }
        return inspectResourceVoList;
    }

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
        List<ResourceInfo> unavailableResourceInfoList = new ArrayList<>();
        String sql = getResourceCountSql(searchVo, unavailableResourceInfoList);
        if (StringUtils.isBlank(sql)) {
            return null;
        }
        int resourceCount = inspectMapper.getInspectResourceCountNew(sql);
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
                sql = getResourceIdListSql(searchVo, unavailableResourceInfoList);
                if (StringUtils.isBlank(sql)) {
                    continue;
                }
                List<Long> resourceIdList = inspectMapper.getInspectResourceIdListNew(sql);
//                List<Long> resourceIdList = inspectMapper.getInspectResourceIdList(searchVo);
                sql = getResourceListByIdListSql(resourceIdList, unavailableResourceInfoList);
                if (StringUtils.isBlank(sql)) {
                    continue;
                }
                List<InspectResourceVo> inspectResourceVos = inspectMapper.getInspectResourceListByIdListNew(sql);
                addInspectResourceOtherInfo(inspectResourceVos);
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
    public String getResourceIdListSql(ResourceSearchVo searchVo, List<ResourceInfo> unavailableResourceInfoList) {
        //查出资源中心数据初始化配置信息来创建ResourceSearchGenerateSqlUtil对象
        IResourceCenterResourceCrossoverService resourceCenterResourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
        List<ResourceEntityVo> resourceEntityList = resourceCenterResourceCrossoverService.getResourceEntityList();
        ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil = new ResourceSearchGenerateSqlUtil(resourceEntityList);
        PlainSelect filterPlainSelect = getPlainSelectBySearchCondition(searchVo, resourceSearchGenerateSqlUtil, unavailableResourceInfoList);
        if (filterPlainSelect == null) {
            return null;
        }
        Table fromTable = (Table)filterPlainSelect.getFromItem();
        List<OrderByElement> orderByElements = new ArrayList<>();
        OrderByElement orderByElement = new OrderByElement();
        orderByElement.withExpression(new Column(fromTable, "id")).withAsc(true);
        orderByElements.add(orderByElement);
        filterPlainSelect.withOrderByElements(orderByElements);
        filterPlainSelect.withDistinct(new Distinct()).setSelectItems(Arrays.asList((new SelectExpressionItem(new Column(fromTable, "id")))));
        filterPlainSelect.withLimit(new Limit().withOffset(new LongValue(searchVo.getStartNum())).withRowCount(new LongValue(searchVo.getPageSize())));
        return filterPlainSelect.toString();
    }

    @Override
    public String getResourceCountSql(ResourceSearchVo searchVo, List<ResourceInfo> unavailableResourceInfoList) {
        //查出资源中心数据初始化配置信息来创建ResourceSearchGenerateSqlUtil对象
        IResourceCenterResourceCrossoverService resourceCenterResourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
        List<ResourceEntityVo> resourceEntityList = resourceCenterResourceCrossoverService.getResourceEntityList();
        ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil = new ResourceSearchGenerateSqlUtil(resourceEntityList);
        PlainSelect filterPlainSelect = getPlainSelectBySearchCondition(searchVo, resourceSearchGenerateSqlUtil, unavailableResourceInfoList);
        if (filterPlainSelect == null) {
            return null;
        }
        Table fromTable = (Table)filterPlainSelect.getFromItem();
        filterPlainSelect.setSelectItems(Arrays.asList(new SelectExpressionItem(new Function().withName("COUNT").withDistinct(true).withParameters(new ExpressionList(Arrays.asList(new Column(fromTable, "id")))))));
        return filterPlainSelect.toString();
    }

    @Override
    public String getResourceListByIdListSql(List<Long> idList, List<ResourceInfo> unavailableResourceInfoList) {
        //查出资源中心数据初始化配置信息来创建ResourceSearchGenerateSqlUtil对象
        IResourceCenterResourceCrossoverService resourceCenterResourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
        List<ResourceEntityVo> resourceEntityList = resourceCenterResourceCrossoverService.getResourceEntityList();
        ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil = new ResourceSearchGenerateSqlUtil(resourceEntityList);
        PlainSelect plainSelect = resourceSearchGenerateSqlUtil.initPlainSelectByMainResourceId("resource_ipobject");
        if (plainSelect == null) {
            return null;
        }
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
        for (ResourceInfo resourceInfo : theadList) {
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
            } else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
        if (CollectionUtils.isNotEmpty(idList)) {
            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(new Column((Table) plainSelect.getFromItem(), "id"));
            ExpressionList expressionList = new ExpressionList();
            for (Object id : idList) {
                if (id instanceof Long) {
                    expressionList.addExpressions(new LongValue((Long)id));
                } else if (id instanceof String) {
                    expressionList.addExpressions(new StringValue((String)id));
                }
            }
            inExpression.setRightItemsList(expressionList);
            plainSelect.setWhere(inExpression);
        }
        return plainSelect.toString();
    }

    /**
     * 根据查询过滤条件，生成对应的sql语句
     * @param searchVo
     * @param resourceSearchGenerateSqlUtil
     * @return
     */
    private PlainSelect getPlainSelectBySearchCondition(ResourceSearchVo searchVo, ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, List<ResourceInfo> unavailableResourceInfoList) {
        PlainSelect plainSelect = resourceSearchGenerateSqlUtil.initPlainSelectByMainResourceId("resource_ipobject");
        if (plainSelect == null) {
            return null;
        }
        Table mainTable = (Table) plainSelect.getFromItem();

        Map<String, ResourceInfo> searchConditionMappingMap = new HashMap<>();
        searchConditionMappingMap.put("typeIdList", new ResourceInfo("resource_ipobject","type_id", false));
        searchConditionMappingMap.put("stateIdList", new ResourceInfo("resource_ipobject_state","state_id", false));
        searchConditionMappingMap.put("envIdList", new ResourceInfo("resource_softwareservice_env","env_id", false));
        searchConditionMappingMap.put("appSystemIdList", new ResourceInfo("resource_appmodule_appsystem","app_system_id", false));
        searchConditionMappingMap.put("appModuleIdList", new ResourceInfo("resource_ipobject_appmodule","app_module_id", false));
        searchConditionMappingMap.put("defaultValue", new ResourceInfo("resource_ipobject","id", false));
        searchConditionMappingMap.put("inspectStatusList", new ResourceInfo("resource_ipobject","inspect_status", false));
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            ResourceInfo resourceInfo = searchConditionMappingMap.get("defaultValue");
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                InExpression inExpression = new InExpression();
                inExpression.setLeftExpression(column);
                ExpressionList expressionList = new ExpressionList();
                for (Long id : idList) {
                    expressionList.addExpressions(new LongValue(id));
                }
                inExpression.setRightItemsList(expressionList);
                Expression where = plainSelect.getWhere();
                if (where == null) {
                    plainSelect.setWhere(inExpression);
                } else {
                    plainSelect.setWhere(new AndExpression(where, inExpression));
                }
            } else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
        List<Long> typeIdList = searchVo.getTypeIdList();
        if (CollectionUtils.isNotEmpty(typeIdList)) {
            ResourceInfo resourceInfo = searchConditionMappingMap.get("typeIdList");
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                InExpression inExpression = new InExpression();
                inExpression.setLeftExpression(column);
                ExpressionList expressionList = new ExpressionList();
                for (Long id : typeIdList) {
                    expressionList.addExpressions(new LongValue(id));
                }
                inExpression.setRightItemsList(expressionList);
                Expression where = plainSelect.getWhere();
                if (where == null) {
                    plainSelect.setWhere(inExpression);
                } else {
                    plainSelect.setWhere(new AndExpression(where, inExpression));
                }
            }else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
        List<String> inspectStatusList = searchVo.getInspectStatusList();
        if (CollectionUtils.isNotEmpty(inspectStatusList)) {
            ResourceInfo resourceInfo = searchConditionMappingMap.get("inspectStatusList");
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                InExpression inExpression = new InExpression();
                inExpression.setLeftExpression(column);
                ExpressionList expressionList = new ExpressionList();
                for (String inspectStatus : inspectStatusList) {
                    expressionList.addExpressions(new StringValue(inspectStatus));
                }
                inExpression.setRightItemsList(expressionList);
                Expression where = plainSelect.getWhere();
                if (where == null) {
                    plainSelect.setWhere(inExpression);
                } else {
                    plainSelect.setWhere(new AndExpression(where, inExpression));
                }
            } else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
        List<Long> stateIdList = searchVo.getStateIdList();
        if (CollectionUtils.isNotEmpty(stateIdList)) {
            ResourceInfo resourceInfo = searchConditionMappingMap.get("stateIdList");
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                InExpression inExpression = new InExpression();
                inExpression.setLeftExpression(column);
                ExpressionList expressionList = new ExpressionList();
                for (Long id : stateIdList) {
                    expressionList.addExpressions(new LongValue(id));
                }
                inExpression.setRightItemsList(expressionList);
                Expression where = plainSelect.getWhere();
                if (where == null) {
                    plainSelect.setWhere(inExpression);
                } else {
                    plainSelect.setWhere(new AndExpression(where, inExpression));
                }
            } else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
        List<Long> envIdList = searchVo.getEnvIdList();
        if (CollectionUtils.isNotEmpty(envIdList)) {
            ResourceInfo resourceInfo = searchConditionMappingMap.get("envIdList");
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                InExpression inExpression = new InExpression();
                inExpression.setLeftExpression(column);
                ExpressionList expressionList = new ExpressionList();
                for (Long id : envIdList) {
                    expressionList.addExpressions(new LongValue(id));
                }
                inExpression.setRightItemsList(expressionList);
                Expression where = plainSelect.getWhere();
                if (where == null) {
                    plainSelect.setWhere(inExpression);
                } else {
                    plainSelect.setWhere(new AndExpression(where, inExpression));
                }
            } else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
        List<Long> appModuleIdList = searchVo.getAppModuleIdList();
        List<Long> appSystemIdList = searchVo.getAppSystemIdList();
        if (CollectionUtils.isNotEmpty(appModuleIdList) || CollectionUtils.isNotEmpty(appSystemIdList)) {
            ResourceInfo resourceInfo = searchConditionMappingMap.get("appModuleIdList");
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                if (CollectionUtils.isNotEmpty(appModuleIdList)) {
                    InExpression inExpression = new InExpression();
                    inExpression.setLeftExpression(column);
                    ExpressionList expressionList = new ExpressionList();
                    for (Long id : appModuleIdList) {
                        expressionList.addExpressions(new LongValue(id));
                    }
                    inExpression.setRightItemsList(expressionList);
                    Expression where = plainSelect.getWhere();
                    if (where == null) {
                        plainSelect.setWhere(inExpression);
                    } else {
                        plainSelect.setWhere(new AndExpression(where, inExpression));
                    }
                }
            } else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
        if (CollectionUtils.isNotEmpty(appSystemIdList)) {
            ResourceInfo resourceInfo = searchConditionMappingMap.get("appSystemIdList");
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                InExpression inExpression = new InExpression();
                inExpression.setLeftExpression(column);
                ExpressionList expressionList = new ExpressionList();
                for (Long id : appSystemIdList) {
                    expressionList.addExpressions(new LongValue(id));
                }
                inExpression.setRightItemsList(expressionList);
                Expression where = plainSelect.getWhere();
                if (where == null) {
                    plainSelect.setWhere(inExpression);
                } else {
                    plainSelect.setWhere(new AndExpression(where, inExpression));
                }
            } else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
        List<Long> protocolIdList = searchVo.getProtocolIdList();
        if (CollectionUtils.isNotEmpty(protocolIdList)) {
            Table table = new Table("cmdb_resourcecenter_resource_account").withAlias(new Alias("b").withUseAs(false));
            EqualsTo equalsTo = new EqualsTo()
                    .withLeftExpression(new Column(table, "resource_id"))
                    .withRightExpression(new Column(mainTable, "id"));
            Join join = new Join().withRightItem(table).addOnExpression(equalsTo);
            plainSelect.addJoins(join);

            Table table2 = new Table("cmdb_resourcecenter_account").withAlias(new Alias("c").withUseAs(false));
            EqualsTo equalsTo1 = new EqualsTo()
                    .withLeftExpression(new Column(table2, "id"))
                    .withRightExpression(new Column(table, "account_id"));
            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(new Column(table2, "protocol_id"));
            ExpressionList expressionList = new ExpressionList();
            for (Long protocolId : protocolIdList) {
                expressionList.addExpressions(new LongValue(protocolId));
            }
            inExpression.setRightItemsList(expressionList);
            Join join2 = new Join().withRightItem(table2).addOnExpression(new AndExpression(equalsTo1, inExpression));
            plainSelect.addJoins(join2);
        }
        List<Long> tagIdList = searchVo.getTagIdList();
        if (CollectionUtils.isNotEmpty(tagIdList)) {
            Table table = new Table("cmdb_resourcecenter_resource_tag").withAlias(new Alias("d").withUseAs(false));
            EqualsTo equalsTo = new EqualsTo()
                    .withLeftExpression(new Column(table, "resource_id"))
                    .withRightExpression(new Column(mainTable, "id"));
            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(new Column(table, "tag_id"));
            ExpressionList expressionList = new ExpressionList();
            for (Long tagId : tagIdList) {
                expressionList.addExpressions(new LongValue(tagId));
            }
            inExpression.setRightItemsList(expressionList);
            Join join1 = new Join().withRightItem(table).addOnExpression(new AndExpression(equalsTo, inExpression));
            plainSelect.addJoins(join1);
        }
        List<String> inspectJobPhaseNodeStatusList = searchVo.getInspectJobPhaseNodeStatusList();
        if (CollectionUtils.isNotEmpty(inspectJobPhaseNodeStatusList)) {
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
        String keyword = searchVo.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            List<ResourceInfo> keywordList = new ArrayList<>();
            keywordList.add(new ResourceInfo("resource_ipobject", "name"));
            keywordList.add(new ResourceInfo("resource_ipobject", "ip"));
            keywordList.add(new ResourceInfo("resource_softwareservice", "port"));
            keyword = "%" + keyword + "%";
            List<Expression> expressionList = new ArrayList<>();
            for (ResourceInfo resourceInfo : keywordList) {
                if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                    Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                    expressionList.add(new LikeExpression().withLeftExpression(column).withRightExpression(new StringValue(keyword)));
                } else {
                    unavailableResourceInfoList.add(resourceInfo);
                }
            }
            MultiOrExpression multiOrExpression = new MultiOrExpression(expressionList);
            Expression where = plainSelect.getWhere();
            if (where == null) {
                plainSelect.setWhere(multiOrExpression);
            } else {
                plainSelect.setWhere(new AndExpression(where, multiOrExpression));
            }
        }
        return plainSelect;
    }

    @Override
    public void addInspectResourceOtherInfo(List<InspectResourceVo> inspectResourceList) {
        List<Long> idList = inspectResourceList.stream().map(InspectResourceVo::getId).collect(Collectors.toList());
        IResourceCenterResourceCrossoverService resourceCenterResourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
        Map<Long, List<AccountVo>> resourceAccountVoMap = resourceCenterResourceCrossoverService.getResourceAccountByResourceIdList(idList);
        Map<Long, List<TagVo>> resourceTagVoMap = resourceCenterResourceCrossoverService.getResourceTagByResourceIdList(idList);
        List<InspectResourceScriptVo> resourceScriptVoList = inspectMapper.getResourceScriptListByResourceIdList(idList);
        Map<Long, InspectResourceScriptVo> resourceScriptMap = resourceScriptVoList.stream().collect(Collectors.toMap(e -> e.getResourceId(), e -> e));
        List<AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeList = autoexecJobMapper.getAutoexecJobNodeListByResourceId(idList);
        Map<Long, AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeMap = autoexecJobPhaseNodeList.stream().collect(Collectors.toMap(e -> e.getResourceId(), e -> e));
        for (InspectResourceVo inspectResourceVo : inspectResourceList) {
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
        }
    }
}

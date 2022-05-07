package codedriver.module.inspect.api;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiCrossoverMapper;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.BgVo;
import codedriver.framework.cmdb.dto.resourcecenter.IpVo;
import codedriver.framework.cmdb.dto.resourcecenter.OwnerVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.InspectStatus;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dao.mapper.InspectMapper;
import codedriver.framework.inspect.dto.InspectResourceVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.TimeUtil;
import codedriver.framework.util.excel.ExcelBuilder;
import codedriver.framework.util.excel.SheetBuilder;
import codedriver.module.inspect.service.InspectReportService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.MongoCollection;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/3/1 2:40 下午
 */
@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class InspectNewProblemReportExportApi extends PrivateBinaryStreamApiComponentBase {
    private final static Logger logger = LoggerFactory.getLogger(InspectNewProblemReportExportApi.class);
    @Resource
    MongoTemplate mongoTemplate;

    @Resource
    InspectMapper inspectMapper;

    @Override
    public String getName() {
        return "导出巡检最报告列表";
    }

    @Override
    public String getToken() {
        return "inspect/new/problem/report/export";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Resource
    private InspectReportService inspectReportService;

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "模糊搜索"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true, desc = "类型id"),
            @Param(name = "protocolIdList", type = ApiParamType.JSONARRAY, desc = "协议id列表"),
            @Param(name = "stateIdList", type = ApiParamType.JSONARRAY, desc = "状态id列表"),
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, desc = "环境id列表"),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "应用系统id列表"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "资源类型id列表"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, desc = "标签id列表"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显的资源ID列表"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "巡检状态列表"),
            @Param(name = "inspectJobPhaseNodeStatusList", type = ApiParamType.JSONARRAY, desc = "巡检作业状态列表"),
            @Param(name = "isNeedAlertDetail", type = ApiParamType.ENUM, rule = "1,0", desc = "1:会导出具体告警信息；0：不会导出具体告警信息;默认为0"),
    })
    @Output({
    })
    @Description(desc = "导出巡检最报告列表接口")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ResourceSearchVo searchVo = JSON.toJavaObject(paramObj, ResourceSearchVo.class);
        MongoCollection<Document> collection = null;
        Integer isNeedAlertDetail = paramObj.getInteger("isNeedAlertDetail");
        if (isNeedAlertDetail == null) {
            isNeedAlertDetail = 0;
        }
        Long typeId = searchVo.getTypeId();
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo ciVo = ciCrossoverMapper.getCiById(typeId);
        if (ciVo == null) {
            throw new CiNotFoundException(typeId);
        }
        searchVo.setLft(ciVo.getLft());
        searchVo.setRht(ciVo.getRht());
        searchVo.setPageSize(100);
        searchVo.setCurrentPage(1);
        int resourceCount = inspectMapper.getInspectResourceCount(searchVo);
        searchVo.setRowNum(resourceCount);
        if (resourceCount > 0) {
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
            for (int i = 1; i <= searchVo.getPageCount(); i++) {
                searchVo.setCurrentPage(i);
                List<Long> resourceIdList = inspectMapper.getInspectResourceIdList(searchVo);
                List<InspectResourceVo> inspectResourceVos = inspectMapper.getInspectResourceVoListByIdList(resourceIdList, TenantContext.get().getDataDbName());
                for (InspectResourceVo inspectResourceVo : inspectResourceVos) {
                    if(isNeedAlertDetail == 1) {
                        JSONObject mongoInspectAlertDetail = inspectReportService.getBatchInspectDetailByResourceId(inspectResourceVo.getId(), collection);
                        JSONObject inspectResult = mongoInspectAlertDetail.getJSONObject("inspectResult");
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
                                    sheetBuilder.addData(dataMap);
                                }
                            }else{
                                Map<String, Object> dataMap = new HashMap<>();
                                putCommonDataMap(dataMap, inspectResourceVo);
                                sheetBuilder.addData(dataMap);
                            }
                        }
                    }else{
                        Map<String, Object> dataMap = new HashMap<>();
                        putCommonDataMap(dataMap, inspectResourceVo);
                        sheetBuilder.addData(dataMap);
                    }
                }
            }
            String fileNameEncode = ciVo.getId() + "_" + ciVo.getLabel() + ".xlsx";
            boolean flag = request.getHeader("User-Agent").indexOf("Gecko") > 0;
            if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0 || flag) {
                fileNameEncode = URLEncoder.encode(fileNameEncode, "UTF-8");// IE浏览器
            } else {
                fileNameEncode = new String(fileNameEncode.replace(" ", "").getBytes(StandardCharsets.UTF_8), "ISO8859-1");
            }
            response.setContentType("application/vnd.ms-excel;charset=utf-8");
            response.setHeader("Content-Disposition", " attachment; filename=\"" + fileNameEncode + "\"");
            try (OutputStream os = response.getOutputStream()) {
                workbook.write(os);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
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
        dataMap.put("inspectJobNodeStatus", inspectResourceVo.getJobPhaseNodeVo() != null ? inspectResourceVo.getJobPhaseNodeVo().getStatusVo().getText() : StringUtils.EMPTY);
        dataMap.put("allIpList", CollectionUtils.isNotEmpty(inspectResourceVo.getAllIp()) ? inspectResourceVo.getAllIp().stream().map(IpVo::getIp).collect(Collectors.joining(",")) : StringUtils.EMPTY);
        dataMap.put("bgList", CollectionUtils.isNotEmpty(inspectResourceVo.getBgList()) ? inspectResourceVo.getBgList().stream().map(BgVo::getBgName).collect(Collectors.joining(",")) : StringUtils.EMPTY);
        dataMap.put("ownerList", CollectionUtils.isNotEmpty(inspectResourceVo.getOwnerList()) ? inspectResourceVo.getOwnerList().stream().map(OwnerVo::getUserName).collect(Collectors.joining(",")) : StringUtils.EMPTY);
        dataMap.put("stateName", inspectResourceVo.getStateName());
        dataMap.put("networkArea", inspectResourceVo.getNetworkArea());
        dataMap.put("tagList", inspectResourceVo.getTagList());
        dataMap.put("maintenanceWindow", inspectResourceVo.getMaintenanceWindow());
    }

}

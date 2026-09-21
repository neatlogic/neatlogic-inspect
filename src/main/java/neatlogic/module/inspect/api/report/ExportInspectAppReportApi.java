/*
 * Copyright (C) 2025 TechSure Co., Ltd. All Rights Reserved.
 */
package neatlogic.module.inspect.api.report;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.InspectStatus;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.inspect.exception.InspectAppReportNotReadyException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.binarystream.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.FileUtil;
import neatlogic.framework.util.TimeUtil;
import neatlogic.framework.util.$;
import neatlogic.framework.util.word.TableBuilder;
import neatlogic.framework.util.word.WordBuilder;
import neatlogic.framework.util.word.enums.TableColor;
import neatlogic.framework.util.word.enums.TitleType;
import neatlogic.module.inspect.service.InspectAppJobService;
import neatlogic.module.inspect.service.InspectReportService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.bson.Document;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 导出单次应用巡检的Word汇总报告。
 */
@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ExportInspectAppReportApi extends PrivateBinaryStreamApiComponentBase {

    private static final String TEXT_PREFIX = "nmiar.exportinspectappreportapi.text.";

    @Resource
    private InspectAppJobService inspectAppJobService;

    @Resource
    private InspectReportService inspectReportService;

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "nmiar.exportinspectappreportapi.getname";
    }

    @Override
    public String getToken() {
        return "inspect/app/report/export";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "parentJobId", type = ApiParamType.LONG, isRequired = true, desc = "term.inspect.parentjobid")
    })
    @Description(desc = "nmiar.exportinspectappreportapi.getname")
    /** 根据父作业范围快照生成Word报告。 */
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long parentJobId = paramObj.getLong("parentJobId");
        JSONObject snapshot = inspectAppJobService.getSnapshot(parentJobId);
        AutoexecJobVo parentJob = autoexecJobMapper.getJobInfo(parentJobId);
        if (snapshot == null || parentJob == null) {
            throw new AutoexecJobNotFoundException(parentJobId);
        }
        if (!inspectAppJobService.isReportReady(parentJobId)) {
            throw new InspectAppReportNotReadyException(parentJobId);
        }
        parentJob = autoexecJobMapper.getJobInfo(parentJobId);
        String appName = StringUtils.defaultIfBlank(snapshot.getString("appSystemName"), snapshot.getString("appSystemAbbrName"));
        String fileName = FileUtil.getEncodedFileName(appName + "_" + parentJobId + "_" + $.t(TEXT_PREFIX + "filename") + ".docx");
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        WordBuilder wordBuilder = new WordBuilder();
        wordBuilder.addTitle(TitleType.TILE, $.t(TEXT_PREFIX + "title"));
        addReportInfo(wordBuilder, snapshot, parentJob);
        Map<String, JSONArray> categoryMap = groupAssets(snapshot.getJSONArray("assetList"));
        int categoryIndex = 1;
        for (Map.Entry<String, JSONArray> entry : categoryMap.entrySet()) {
            wordBuilder.addTitle(TitleType.H1, categoryIndex++ + ". " + entry.getKey());
            JSONArray problemList = addAssetTable(wordBuilder, entry.getValue());
            wordBuilder.addTitle(TitleType.H2, $.t(TEXT_PREFIX + "problemdetail"));
            if (problemList.isEmpty()) {
                wordBuilder.addParagraph($.t(TEXT_PREFIX + "noproblem"));
            } else {
                addProblemTable(wordBuilder, problemList);
            }
        }
        try (XWPFDocument document = wordBuilder.builder(); OutputStream outputStream = response.getOutputStream()) {
            document.write(outputStream);
            outputStream.flush();
        }
        return null;
    }

    /** 生成报告基本信息表。 */
    private void addReportInfo(WordBuilder wordBuilder, JSONObject snapshot, AutoexecJobVo parentJob) {
        Map<Integer, String> headerMap = new LinkedHashMap<>();
        headerMap.put(1, $.t(TEXT_PREFIX + "item"));
        headerMap.put(2, $.t(TEXT_PREFIX + "content"));
        TableBuilder table = wordBuilder.addTable(headerMap, TableColor.BLUE);
        addInfoRow(table, $.t(TEXT_PREFIX + "application"), snapshot.getString("appSystemName"));
        addInfoRow(table, $.t(TEXT_PREFIX + "parentjobid"), snapshot.getString("parentJobId"));
        addInfoRow(table, $.t(TEXT_PREFIX + "environment"), joinScopeNames(snapshot.getJSONArray("envList"), "envName", "envId"));
        addInfoRow(table, $.t(TEXT_PREFIX + "initiator"), parentJob.getExecUser());
        addInfoRow(table, $.t(TEXT_PREFIX + "starttime"), formatDate(parentJob.getStartTime()));
        addInfoRow(table, $.t(TEXT_PREFIX + "endtime"), formatDate(parentJob.getEndTime()));
        addInfoRow(table, $.t(TEXT_PREFIX + "jobstatus"), parentJob.getStatusName());
        wordBuilder.addBlankRow();
    }

    /** 添加一行报告基本信息。 */
    private void addInfoRow(TableBuilder table, String key, String value) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put($.t(TEXT_PREFIX + "item"), key);
        row.put($.t(TEXT_PREFIX + "content"), StringUtils.defaultString(value, "-"));
        table.addRow(row);
    }

    /** 按创建时保存的分类顺序组织资产。 */
    private Map<String, JSONArray> groupAssets(JSONArray assetList) {
        Map<String, JSONArray> categoryMap = new LinkedHashMap<>();
        if (CollectionUtils.isNotEmpty(assetList)) {
            for (int i = 0; i < assetList.size(); i++) {
                JSONObject asset = assetList.getJSONObject(i);
                String category = StringUtils.defaultIfBlank(asset.getString("category"), asset.getString("typeLabel"));
                categoryMap.computeIfAbsent(category, key -> new JSONArray()).add(asset);
            }
        }
        return categoryMap;
    }

    /** 添加分类资产状态表并返回问题明细。 */
    private JSONArray addAssetTable(WordBuilder wordBuilder, JSONArray assetList) {
        Map<Integer, String> headerMap = new LinkedHashMap<>();
        headerMap.put(1, $.t(TEXT_PREFIX + "assetname"));
        headerMap.put(2, $.t(TEXT_PREFIX + "ipport"));
        headerMap.put(3, $.t(TEXT_PREFIX + "module"));
        headerMap.put(4, $.t(TEXT_PREFIX + "environment"));
        headerMap.put(5, $.t(TEXT_PREFIX + "healthstatus"));
        headerMap.put(6, $.t(TEXT_PREFIX + "inspectiontimeorremark"));
        TableBuilder table = wordBuilder.addTable(headerMap, TableColor.BLUE);
        JSONArray problemList = new JSONArray();
        for (int i = 0; i < assetList.size(); i++) {
            JSONObject asset = assetList.getJSONObject(i);
            JSONObject inspection = getInspection(asset);
            Map<String, String> row = new LinkedHashMap<>();
            row.put($.t(TEXT_PREFIX + "assetname"), StringUtils.defaultIfBlank(asset.getString("name"), asset.getString("resourceId")));
            row.put($.t(TEXT_PREFIX + "ipport"), formatIp(asset));
            row.put($.t(TEXT_PREFIX + "module"), StringUtils.defaultString(asset.getString("appModuleName"), "-"));
            row.put($.t(TEXT_PREFIX + "environment"), StringUtils.defaultString(asset.getString("envName"), "-"));
            row.put($.t(TEXT_PREFIX + "healthstatus"), inspection.getString("statusText"));
            row.put($.t(TEXT_PREFIX + "inspectiontimeorremark"), StringUtils.defaultIfBlank(inspection.getString("reportTime"), inspection.getString("reason")));
            table.addRow(row);
            JSONArray alerts = inspection.getJSONArray("alerts");
            if (CollectionUtils.isNotEmpty(alerts) || StringUtils.isNotBlank(inspection.getString("reason"))) {
                JSONObject problem = new JSONObject();
                problem.put("asset", asset);
                problem.put("inspection", inspection);
                problemList.add(problem);
            }
        }
        wordBuilder.addBlankRow();
        return problemList;
    }

    /** 读取本次子作业产生的历史报告并转换为汇总信息。 */
    private JSONObject getInspection(JSONObject asset) {
        JSONObject result = new JSONObject();
        JSONArray alerts = new JSONArray();
        result.put("alerts", alerts);
        Long jobId = asset.getLong("jobId");
        String reason = asset.getString("uninspectedReason");
        if (jobId == null) {
            result.put("statusText", $.t(TEXT_PREFIX + "uninspected"));
            result.put("reason", StringUtils.defaultIfBlank(reason, $.t(TEXT_PREFIX + "jobnotcreated")));
            return result;
        }
        Document reportDocument = inspectReportService.getInspectReport(asset.getLong("resourceId"), null, jobId);
        if (reportDocument == null || reportDocument.isEmpty()) {
            result.put("statusText", $.t(TEXT_PREFIX + "noresult"));
            result.put("reason", StringUtils.defaultIfBlank(reason, $.t(TEXT_PREFIX + "reportnotgenerated")));
            return result;
        }
        JSONObject report = JSONObject.parseObject(reportDocument.toJson());
        JSONObject inspectResult = report.getJSONObject("_inspect_result");
        JSONArray alertFields = inspectResult == null ? null : inspectResult.getJSONArray("alertFields");
        String worstStatus = "normal";
        if (CollectionUtils.isNotEmpty(alertFields)) {
            Map<String, String> translationMap = getTranslationMap(report.getJSONArray("fields"));
            for (int i = 0; i < alertFields.size(); i++) {
                JSONObject alert = alertFields.getJSONObject(i);
                String level = StringUtils.lowerCase(alert.getString("alertLevel"), Locale.ROOT);
                worstStatus = getWorseStatus(worstStatus, level);
                JSONObject item = new JSONObject();
                item.put("level", InspectStatus.getText(level));
                item.put("field", translateField(alert.getString("alertField"), translationMap));
                JSONArray ruleNames = alert.getJSONArray("ruleNames");
                item.put("message", CollectionUtils.isEmpty(ruleNames) ? "-" : String.join(", ", ruleNames.toJavaList(String.class)));
                alerts.add(item);
            }
        }
        result.put("statusText", InspectStatus.getText(worstStatus));
        Object reportTime = report.get("_report_time");
        if (reportTime instanceof JSONObject) {
            result.put("reportTime", formatDate(((JSONObject) reportTime).getDate("$date")));
        }
        return result;
    }

    /** 添加问题资产及告警明细表。 */
    private void addProblemTable(WordBuilder wordBuilder, JSONArray problemList) {
        Map<Integer, String> headerMap = new LinkedHashMap<>();
        headerMap.put(1, $.t(TEXT_PREFIX + "asset"));
        headerMap.put(2, $.t(TEXT_PREFIX + "alertlevel"));
        headerMap.put(3, $.t(TEXT_PREFIX + "inspectionitem"));
        headerMap.put(4, $.t(TEXT_PREFIX + "problemdescription"));
        TableBuilder table = wordBuilder.addTable(headerMap, TableColor.BLUE);
        for (int i = 0; i < problemList.size(); i++) {
            JSONObject problem = problemList.getJSONObject(i);
            JSONObject asset = problem.getJSONObject("asset");
            JSONObject inspection = problem.getJSONObject("inspection");
            JSONArray alerts = inspection.getJSONArray("alerts");
            if (CollectionUtils.isEmpty(alerts)) {
                addProblemRow(table, asset.getString("name"), inspection.getString("statusText"), "-", inspection.getString("reason"));
            } else {
                for (int j = 0; j < alerts.size(); j++) {
                    JSONObject alert = alerts.getJSONObject(j);
                    addProblemRow(table, asset.getString("name"), alert.getString("level"), alert.getString("field"), alert.getString("message"));
                }
            }
        }
    }

    /** 添加一行问题明细。 */
    private void addProblemRow(TableBuilder table, String asset, String level, String field, String message) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put($.t(TEXT_PREFIX + "asset"), StringUtils.defaultString(asset, "-"));
        row.put($.t(TEXT_PREFIX + "alertlevel"), StringUtils.defaultString(level, "-"));
        row.put($.t(TEXT_PREFIX + "inspectionitem"), StringUtils.defaultString(field, "-"));
        row.put($.t(TEXT_PREFIX + "problemdescription"), StringUtils.defaultString(message, "-"));
        table.addRow(row);
    }

    /** 生成字段路径译文映射。 */
    private Map<String, String> getTranslationMap(JSONArray fields) {
        Map<String, String> result = new LinkedHashMap<>();
        appendTranslation(result, null, fields);
        return result;
    }

    /** 递归展开字段定义。 */
    private void appendTranslation(Map<String, String> result, String parent, JSONArray fields) {
        if (CollectionUtils.isEmpty(fields)) {
            return;
        }
        for (int i = 0; i < fields.size(); i++) {
            JSONObject field = fields.getJSONObject(i);
            String name = field.getString("name");
            String path = StringUtils.isBlank(parent) ? name : parent + "." + name;
            result.put(path, field.getString("desc"));
            appendTranslation(result, path, field.getJSONArray("subset"));
        }
    }

    /** 将告警JSONPath转换为巡检项名称。 */
    private String translateField(String alertField, Map<String, String> translationMap) {
        if (StringUtils.isBlank(alertField)) {
            return "-";
        }
        String path = alertField.replaceFirst("^\\$\\.", "").replaceAll("\\[[0-9]+]", "");
        return StringUtils.defaultIfBlank(translationMap.get(path), path);
    }

    /** 按正常、告警、严重、致命的顺序返回更严重状态。 */
    private String getWorseStatus(String current, String candidate) {
        List<String> order = List.of("normal", "warn", "critical", "fatal");
        return order.indexOf(candidate) > order.indexOf(current) ? candidate : current;
    }

    /** 拼接环境等范围名称。 */
    private String joinScopeNames(JSONArray list, String nameKey, String idKey) {
        if (CollectionUtils.isEmpty(list)) {
            return $.t(TEXT_PREFIX + "all");
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            JSONObject item = list.getJSONObject(i);
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(StringUtils.defaultIfBlank(item.getString(nameKey), item.getString(idKey)));
        }
        return builder.toString();
    }

    /** 格式化IP与端口。 */
    private String formatIp(JSONObject asset) {
        String ip = StringUtils.defaultString(asset.getString("ip"));
        return asset.getInteger("port") == null ? ip : ip + ":" + asset.getInteger("port");
    }

    /** 格式化报告时间。 */
    private String formatDate(Date date) {
        return date == null ? "-" : TimeUtil.convertDateToString(date, TimeUtil.YYYY_MM_DD_HH_MM_SS);
    }
}

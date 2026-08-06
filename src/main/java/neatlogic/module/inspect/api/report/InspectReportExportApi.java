/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.asynchronization.threadlocal.RequestContext;
import neatlogic.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.binarystream.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.DocType;
import neatlogic.framework.util.ExportUtil;
import neatlogic.framework.util.FreemarkerUtil;
import neatlogic.framework.util.TimeUtil;
import neatlogic.framework.util.$;
import neatlogic.module.inspect.service.InspectReportService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Service
public class InspectReportExportApi extends PrivateBinaryStreamApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(InspectReportExportApi.class);

    private static final String I18N_KEY_PREFIX = "nmiar.inspectreportexportapi.";
    private static final String TEXT_KEY_PREFIX = I18N_KEY_PREFIX + "text.";

    static String template;

    static {
        try (InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(InspectReportExportApi.class.getClassLoader()
                .getResourceAsStream("template/inspect-report-template.ftl")), StandardCharsets.UTF_8)) {
            template = IOUtils.toString(reader);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Resource
    private UserMapper userMapper;

    @Resource
    private InspectReportService inspectReportService;

    @Override
    public String getName() {
        return I18N_KEY_PREFIX + "getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "inspect/report/export";
    }


    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = I18N_KEY_PREFIX + "input.param.desc.resourceid", isRequired = true),
            @Param(name = "id", type = ApiParamType.STRING, desc = I18N_KEY_PREFIX + "input.param.desc.id"),
            @Param(name = "jobId", type = ApiParamType.STRING, desc = I18N_KEY_PREFIX + "input.param.desc.jobid"),
            @Param(name = "type", type = ApiParamType.ENUM, rule = "word,pdf", desc = I18N_KEY_PREFIX + "input.param.desc.type", isRequired = true)
    })
    @Description(desc = I18N_KEY_PREFIX + "description")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long resourceId = paramObj.getLong("resourceId");
        String id = paramObj.getString("id");
        Long jobId = paramObj.getLong("jobId");
        String type = paramObj.getString("type");
        boolean isEnglish = isEnglishLocale();
        String noDataText = $.t(TEXT_KEY_PREFIX + "nodata");
        IResourceCenterResourceCrossoverService resourceCenterResourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
        ResourceVo resource = resourceCenterResourceCrossoverService.getResourceById(resourceId);
        String fileName = resourceId.toString();
        if (resource != null && resource.getName() != null) {
            fileName = resource.getName();
        }
        Document reportDocument = inspectReportService.getInspectReport(resourceId, id, jobId);
        JSONObject reportDoc = null;
        if (MapUtils.isNotEmpty(reportDocument)) {
            reportDoc = JSONObject.parseObject(reportDocument.toJson());
        }
        if (MapUtils.isNotEmpty(reportDoc)) {
            Map<String, String> translationMap = new HashMap<>();
            JSONArray fields = reportDoc.getJSONArray("fields");
            if (CollectionUtils.isNotEmpty(fields)) {
                for (int i = 0; i < fields.size(); i++) {
                    JSONObject obj = fields.getJSONObject(i);
                    String name = obj.getString("name");
                    translationMap.put(name, getFieldLabel(obj, isEnglish));
                    recursionForTranslation(translationMap, name, obj.getJSONArray("subset"), isEnglish);
                }
            }
            JSONObject alert = null;
            Map<String, String> alertMap = new HashMap<>(); // 记录jsonpath与告警级别之间的映射
            Map<String, String> alertLevelClassMap = new HashMap<>();
            JSONObject inspectStatus = reportDoc.getJSONObject("inspectStatus");
            // 组装告警级别与cssClass之间的映射(alertLevelClassMap)和告警提示(alert)
            if (MapUtils.isNotEmpty(inspectStatus)) {
                for (String key : inspectStatus.keySet()) {
                    JSONObject object = inspectStatus.getJSONObject(key);
                    alertLevelClassMap.put(object.getString("value"), object.getString("cssClass"));
                }
                alert = getAlert(
                        reportDoc,
                        translationMap,
                        alertMap,
                        inspectStatus,
                        $.t(TEXT_KEY_PREFIX + "alertlevel"),
                        $.t(TEXT_KEY_PREFIX + "alertfield"),
                        $.t(TEXT_KEY_PREFIX + "alertmessage"),
                        $.t(TEXT_KEY_PREFIX + "rulenameseparator")
                );
            }

            JSONArray lineList = new JSONArray();
            JSONArray tableList = new JSONArray();
            getDataMap(reportDoc, translationMap, alertMap, lineList, tableList, noDataText);
            JSONObject dataObj = new JSONObject();
            if (MapUtils.isNotEmpty(alert)) {
                dataObj.put("alert", alert);
                dataObj.put("alertTitle", $.t(TEXT_KEY_PREFIX + "alert"));
            }
            if (!alertLevelClassMap.isEmpty()) {
                dataObj.put("alertLevelClassMap", alertLevelClassMap);
            }
            dataObj.put("lineList", lineList);
            dataObj.put("tableList", tableList);
            String execUser = reportDoc.getString("_execuser");
            Date reportTime = null;
            Object reportTimeObj = reportDoc.get("_report_time");
            if (reportTimeObj instanceof Date) {
                reportTime = (Date) reportTimeObj;
            } else if (reportTimeObj instanceof Long) {
                reportTime = new Date((Long)reportTimeObj);
            }
            if (StringUtils.isNotBlank(execUser)) {
                UserVo userVo = userMapper.getUserBaseInfoByUuid(execUser);
                if (userVo != null) {
                    dataObj.put("execUser", userVo.getUserName());
                }
            }
            if (reportTime != null) {
                dataObj.put("reportTime", TimeUtil.convertDateToString(reportTime, TimeUtil.YYYY_MM_DD_HH_MM_SS));
            }
            dataObj.put("reportName", fileName + "_" + $.t(TEXT_KEY_PREFIX + "reporttitle"));
            fileName += "_" + $.t(TEXT_KEY_PREFIX + "reportfilesuffix");
            dataObj.put("docType", type);
            String content = FreemarkerUtil.transform(dataObj, template);
            try (OutputStream os = response.getOutputStream()) {
                if (DocType.WORD.getValue().equals(type)) {
                    response.setCharacterEncoding("utf-8");
                    response.setContentType("application/msword");
                    response.setHeader("Content-Disposition",
                            " attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()) + ".docx\"");
                    ExportUtil.getWordFileByHtml(content, os, true, false);
                    os.flush();
                } else if (DocType.PDF.getValue().equals(type)) {
                    response.setContentType("application/pdf");
                    response.setHeader("Content-Disposition",
                            " attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()) + ".pdf\"");
                    ExportUtil.savePdf(content, os, false);
                    os.flush();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        return null;
    }

    private boolean isEnglishLocale() {
        Locale locale = RequestContext.get() != null ? RequestContext.get().getLocale() : null;
        return locale != null && Locale.ENGLISH.getLanguage().equalsIgnoreCase(locale.getLanguage());
    }

    /**
     * 巡检字段定义的desc为中文描述，英文环境直接使用稳定的字段name。
     */
    private String getFieldLabel(JSONObject field, boolean isEnglish) {
        return field.getString(isEnglish ? "name" : "desc");
    }

    /**
     * 组装告警列表，将jsonpath转换为当前系统语言对应的字段路径。
     * 并且记录jsonpath与告警级别之间的映射
     *
     * @param reportDoc      document
     * @param translationMap 译文
     * @param alertMap       jsonpath与告警级别之间的映射
     * @param inspectStatus  inspectStatus
     * @return
     */
    private JSONObject getAlert(
            JSONObject reportDoc,
            Map<String, String> translationMap,
            Map<String, String> alertMap,
            JSONObject inspectStatus,
            String alertLevelText,
            String alertFieldText,
            String alertMessageText,
            String ruleNameSeparator
    ) {
        JSONObject inspectResult = reportDoc.getJSONObject("_inspect_result");
        if (inspectResult != null) {
            JSONArray alertFields = inspectResult.getJSONArray("alertFields");
            if (CollectionUtils.isNotEmpty(alertFields)) {
                JSONObject alert = new JSONObject();
                JSONArray headList = new JSONArray();
                JSONObject levelHead = new JSONObject();
                levelHead.put("key", "levelText");
                levelHead.put("title", alertLevelText);
                headList.add(levelHead);
                JSONObject fieldHead = new JSONObject();
                fieldHead.put("key", "fieldText");
                fieldHead.put("title", alertFieldText);
                headList.add(fieldHead);
                JSONObject messageHead = new JSONObject();
                messageHead.put("key", "messageText");
                messageHead.put("title", alertMessageText);
                headList.add(messageHead);
                alert.put("headList", headList);
                JSONArray alertArray = new JSONArray();
                alert.put("rowList", alertArray);
                for (int i = 0; i < alertFields.size(); i++) {
                    JSONObject alertObj = new JSONObject();
                    JSONObject object = alertFields.getJSONObject(i);
                    String alertField = object.getString("alertField").split("\\$\\.")[1];
                    alertMap.put(alertField, object.getString("alertLevel").toLowerCase(Locale.ROOT));
                    String field;
                    if (alertField.contains("[")) {
                        alertField = alertField.replaceAll("\\[.\\]", "");
                        String[] split = alertField.split("\\.");
                        StringBuilder sb = new StringBuilder();
                        for (int j = 0; j < split.length; j++) {
                            String key;
                            if (j != 0) {
                                key = split[j - 1] + "." + split[j];
                            } else {
                                key = split[j];
                            }
                            sb.append(translationMap.get(key));
                            if (j != split.length - 1) {
                                sb.append("->");
                            }
                        }
                        field = sb.toString();
                    } else {
                        field = translationMap.get(alertField);
                    }
                    alertObj.put("fieldText", field);
                    String level = object.getString("alertLevel").toLowerCase(Locale.ROOT);
                    alertObj.put("level", level);
                    JSONObject alertLevel = inspectStatus.getJSONObject(level);
                    if (MapUtils.isNotEmpty(alertLevel)) {
                        alertObj.put("levelText", alertLevel.getString("text"));
                    }
                    JSONArray ruleNameArray = object.getJSONArray("ruleNames");
                    if (CollectionUtils.isNotEmpty(ruleNameArray)) {
                        List<String> ruleNames = ruleNameArray.toJavaList(String.class);
                        alertObj.put("messageText", String.join(ruleNameSeparator, ruleNames));
                    }
                    alertArray.add(alertObj);
                }
                return alert;
            }
        }
        return null;
    }

    /**
     * 解析MongoDB Document
     *
     * @param reportJson     待解析的document
     * @param translationMap 译文
     * @param alertMap       jsonpath与告警级别之间的映射
     * @param lineList       存储String、int或Array字段的list
     * @param tableList      存储JsonArray字段的list
     */
    private void getDataMap(
            JSONObject reportJson,
            Map<String, String> translationMap,
            Map<String, String> alertMap,
            JSONArray lineList,
            JSONArray tableList,
            String noDataText
    ) {

        JSONArray fields = reportJson.getJSONArray("fields");
        for (int i = 0; i < fields.size(); i++) {
            JSONObject fieldObj = fields.getJSONObject(i);
            if (MapUtils.isEmpty(fieldObj)) {
                continue;
            }
            String key = fieldObj.getString("name");
            if (key.startsWith("_")) {
                continue;
            }
            Object value = reportJson.get(fieldObj.getString("name"));
            Object subset = fieldObj.get("subset");
            String name = translationMap.get(key);
            String alertLevel = alertMap.get(key);
            if (name != null) {
                if (subset instanceof List) {
                    JSONObject table = new JSONObject();
                    JSONArray listValue = new JSONArray();
                    if (value instanceof List) {
                        listValue.addAll((List) value);
                    } else if (value instanceof Map) {
                        Map<String, Object> map = (Map) value;
                        listValue.add(new JSONObject(map));
                        recursionForTable(table, translationMap, alertMap, key, listValue, key, fieldObj, noDataText);
                        tableList.add(table);
                        continue;
                    }

                    if (CollectionUtils.isNotEmpty(listValue)) {
                        if (!(listValue.get(0) instanceof Document)) { // 元素类型不是Document，说明value是非JSONObject数组
                            JSONObject line = new JSONObject();
                            line.put("key", name);
                            line.put("value", listValue.toString());
                            if (alertLevel != null) {
                                line.put("alertLevel", alertLevel);
                            }
                            lineList.add(line);
                        } else {
                            recursionForTable(table, translationMap, alertMap, key, listValue, key, fieldObj, noDataText);
                            tableList.add(table);
                        }
                    } else {
                        JSONObject line = new JSONObject();
                        line.put("key", name);
                        line.put("value", noDataText);
                        lineList.add(line);
                    }
                } else {
                    if (value == null || Objects.equals(StringUtils.EMPTY, value)) {
                        value = noDataText;
                    }
                    if (value instanceof Date) {
                        value = TimeUtil.convertDateToString((Date) value, TimeUtil.YYYY_MM_DD_HH_MM_SS);
                    }
                    JSONObject line = new JSONObject();
                    line.put("key", name);
                    line.put("value", value.toString());
                    if (alertLevel != null) {
                        line.put("alertLevel", alertLevel);
                    }
                    lineList.add(line);
                }
            }
        }
    }

    /**
     * 递归抽取字段译文，如果存在嵌套数组，则转为链式结构
     * 例如：{"name":"DNS_SERVERS","type":"JsonArray","subset":[{"name":"VALUE","type":"String","desc":"IP"}],"desc":"DNS服务器"}
     * 将转为：
     * "DNS_SERVERS" -> "DNS服务器"
     * "DNS_SERVERS.VALUE" -> "IP"
     *
     * @param translationMap
     * @param name
     * @param subset
     */
    private void recursionForTranslation(Map<String, String> translationMap, String name, JSONArray subset, boolean isEnglish) {
        if (CollectionUtils.isNotEmpty(subset)) {
            for (int i = 0; i < subset.size(); i++) {
                JSONObject _obj = subset.getJSONObject(i);
                String _name = _obj.getString("name");
                translationMap.put(name + "." + _name, getFieldLabel(_obj, isEnglish));
                recursionForTranslation(translationMap, name + "." + _name, _obj.getJSONArray("subset"), isEnglish);
            }
        }
    }

    /**
     * 解析JsonArray类型的document字段，组装成如下结构：
     * {"headList":["单位","磁盘名","类型","容量"],"valueList":[{"单位":"GB","磁盘名":"/dev/sda","容量":"137","类型":"local"}],"key":"磁盘"}
     * 如果存在嵌套，则结构如下：
     * {"headList":["网卡名","状态","速率","网卡地址","连接交换机端口"],"valueList":[{"网卡地址":"00:0c:29:e0:ec:e4","网卡名":"eth0","状态":"up","速率":"10000","连接交换机端口":{"headList":["端口名","归属类别","交换机类型","管理Ip","序列号"],"valueList":[{"归属类别":"q","序列号":"r","管理Ip":"e","交换机类型":"w","端口名":"t"}]}}],"key":"网卡"}
     *
     * @param table          转换后的JSONObject
     * @param translationMap 译文
     * @param key            key
     * @param array          待转换的JsonArray字段
     * @param alertKey       jsonpath
     */
    private void recursionForTable(
            JSONObject table,
            Map<String, String> translationMap,
            Map<String, String> alertMap,
            String key,
            JSONArray array,
            String alertKey,
            JSONObject fieldObj,
            String noDataText
    ) {
        Set<String> headSet = new LinkedHashSet<>();
        JSONArray subset = fieldObj.getJSONArray("subset");
        if (CollectionUtils.isEmpty(subset)) {
            return;
        }
        for (Object o : subset) {
            JSONObject jsonObject = (JSONObject) o;
            headSet.add(jsonObject.getString("name"));
        }
        List<String> headList = new ArrayList<>();
        Iterator<String> iterator = headSet.iterator();
        while (iterator.hasNext()) {
            String name = translationMap.get(key + "." + iterator.next());
            if (name != null) {
                headList.add(name);
            } else {
                iterator.remove(); // 抛弃没有译文的字段
            }
        }
        table.put("key", translationMap.get(key));
        table.put("headList", headList);
        JSONArray valueList = new JSONArray();
        table.put("valueList", valueList);
        for (int i = 0; i < array.size(); i++) {
            JSONObject object = array.getJSONObject(i);
            JSONObject row = new JSONObject();
            int j = 0;
            for (String head : headSet) {
                Object obj = object.get(head);
                if (obj != null) {
                    if (obj instanceof List && CollectionUtils.isNotEmpty((List) obj) && ((List) obj).get(0) instanceof Map) {
                        List list = (List) obj;
                        if (CollectionUtils.isNotEmpty(list)) {
                            JSONObject _table = new JSONObject();
                            JSONArray _array = new JSONArray();
                            _array.addAll(list);
                            recursionForTable(_table, translationMap, alertMap, key + "." + head, _array, (alertKey + "[" + i + "]" + "." + head), fieldObj, noDataText);
                            _table.remove("key");
                            row.put(headList.get(j), _table);
                        } else {
                            row.put(headList.get(j), noDataText);
                        }
                    } else {
                        String alertLevel = alertMap.get(alertKey + "[" + i + "]" + "." + head);
                        if (obj instanceof Date) {
                            obj = TimeUtil.convertDateToString((Date) obj, TimeUtil.YYYY_MM_DD_HH_MM_SS);
                        } else if (obj instanceof List) {
                            List list = (List) obj;
                            if (CollectionUtils.isNotEmpty(list)) {
                                obj = String.join(",", list);
                            }
                        }
                        String value = !Objects.equals(obj.toString(), StringUtils.EMPTY) ? obj.toString() : noDataText;
                        if (alertLevel != null) {
                            value += ("&=&" + alertLevel); // 如果有告警，则拼接告警级别到末尾，freemarker解析时，按&=&分割正文与告警级别，根据告警级别确定正文的样式
                        }
                        row.put(headList.get(j), value);
                    }
                } else {
                    row.put(headList.get(j), noDataText);
                }
                j++;
            }
            valueList.add(row);
        }
    }

}

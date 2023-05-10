/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
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
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.DocType;
import neatlogic.framework.util.ExportUtil;
import neatlogic.framework.util.FreemarkerUtil;
import neatlogic.framework.util.TimeUtil;
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

    static String template;

    static {
        try {
            InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(InspectReportExportApi.class.getClassLoader()
                    .getResourceAsStream("template/inspect-report-template.ftl")), StandardCharsets.UTF_8.name());
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
        return "导出巡检报告";
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
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资产id", isRequired = true),
            @Param(name = "id", type = ApiParamType.STRING, desc = "id"),
            @Param(name = "jobId", type = ApiParamType.STRING, desc = "作业id"),
            @Param(name = "type", type = ApiParamType.ENUM, rule = "word,pdf", desc = "类型", isRequired = true)
    })
    @Description(desc = "导出巡检报告")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long resourceId = paramObj.getLong("resourceId");
        String id = paramObj.getString("id");
        Long jobId = paramObj.getLong("jobId");
        String type = paramObj.getString("type");
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo resource = resourceCrossoverMapper.getResourceById(resourceId);
        String fileName = resourceId.toString();
        if (resource != null && resource.getName() != null) {
            fileName = resource.getName();
        }
        Document reportDoc = inspectReportService.getInspectReport(resourceId, id, jobId);
        if (MapUtils.isNotEmpty(reportDoc)) {
            Map<String, String> translationMap = new HashMap<>();
            JSONArray fields = JSON.parseArray(reportDoc.get("fields").toString());
            if (CollectionUtils.isNotEmpty(fields)) {
                for (int i = 0; i < fields.size(); i++) {
                    JSONObject obj = fields.getJSONObject(i);
                    String name = obj.getString("name");
                    String desc = obj.getString("desc");
                    translationMap.put(name, desc);
                    recursionForTranslation(translationMap, name, obj.getJSONArray("subset"));
                }
            }
            JSONObject alert = null;
            Map<String, String> alertMap = new HashMap<>(); // 记录jsonpath与告警级别之间的映射
            Map<String, String> alertLevelClassMap = new HashMap<>();
            Map<String, Object> inspectStatus = (Map<String, Object>) reportDoc.get("inspectStatus");
            // 组装告警级别与cssClass之间的映射(alertLevelClassMap)和告警提示(alert)
            if (MapUtils.isNotEmpty(inspectStatus)) {
                for (Map.Entry<String, Object> entry : inspectStatus.entrySet()) {
                    JSONObject object = JSON.parseObject(entry.getValue().toString());
                    alertLevelClassMap.put(object.getString("value"), object.getString("cssClass"));
                }
                alert = getAlert(reportDoc, translationMap, alertMap, inspectStatus);
            }

            JSONArray lineList = new JSONArray();
            JSONArray tableList = new JSONArray();
            getDataMap(reportDoc, translationMap, alertMap, lineList, tableList);
            JSONObject dataObj = new JSONObject();
            if (MapUtils.isNotEmpty(alert)) {
                dataObj.put("alert", alert);
            }
            if (!alertLevelClassMap.isEmpty()) {
                dataObj.put("alertLevelClassMap", alertLevelClassMap);
            }
            dataObj.put("lineList", lineList);
            dataObj.put("tableList", tableList);
            String execUser = reportDoc.getString("_execuser");
            Date reportTime = reportDoc.getDate("_report_time");
            if (StringUtils.isNotBlank(execUser)) {
                UserVo userVo = userMapper.getUserBaseInfoByUuid(execUser);
                if (userVo != null) {
                    dataObj.put("execUser", userVo.getUserName());
                }
            }
            if (reportTime != null) {
                dataObj.put("reportTime", TimeUtil.convertDateToString(reportTime, TimeUtil.YYYY_MM_DD_HH_MM_SS));
            }
            fileName += "_巡检报告";
            dataObj.put("reportName", fileName);
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

    /**
     * 组装告警列表，将jsonpath转为中文路径，结构如下：
     * {"headList":["告警级别","告警字段","告警提示"],"rowList":[{"level":"normal","告警级别":"正常","告警字段":"挂载点->使用率%","告警提示":"磁盘空间使用率超过11%、磁盘空间使用率超过15%"},{"level":"normal","告警级别":"正常","告警字段":"挂载点->使用率%","告警提示":"磁盘空间使用率超过11%"}]}
     * 并且记录jsonpath与告警级别之间的映射
     *
     * @param reportDoc      document
     * @param translationMap 译文
     * @param alertMap       jsonpath与告警级别之间的映射
     * @param inspectStatus  inspectStatus
     * @return
     */
    private JSONObject getAlert(Document reportDoc, Map<String, String> translationMap, Map<String, String> alertMap, Map<String, Object> inspectStatus) {
        Document inspectResult = (Document) reportDoc.get("_inspect_result");
        if (inspectResult != null) {
            List alertFields = (List) inspectResult.get("alertFields");
            if (CollectionUtils.isNotEmpty(alertFields)) {
                JSONObject alert = new JSONObject();
                JSONArray headList = new JSONArray();
                headList.add("告警级别");
                headList.add("告警字段");
                headList.add("告警提示");
                alert.put("headList", headList);
                JSONArray alertArray = new JSONArray();
                alert.put("rowList", alertArray);
                for (int i = 0; i < alertFields.size(); i++) {
                    JSONObject alertObj = new JSONObject();
                    Document object = (Document) alertFields.get(i);
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
                    alertObj.put("告警字段", field);
                    Object alertLevel = inspectStatus.get(object.getString("alertLevel").toLowerCase(Locale.ROOT));
                    alertObj.put("level", object.getString("alertLevel").toLowerCase(Locale.ROOT));
                    alertObj.put("告警级别", JSON.parseObject(alertLevel.toString()).getString("text"));
                    List<String> ruleNames = (List<String>) object.get("ruleNames");
                    alertObj.put("告警提示", String.join("、", ruleNames));
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
    private void getDataMap(Map<String, Object> reportJson, Map<String, String> translationMap, Map<String, String> alertMap, JSONArray lineList, JSONArray tableList) {

        JSONArray fields = JSON.parseArray(reportJson.get("fields").toString());
        for (int i = 0; i < fields.size(); i++) {
            JSONObject fieldObj = fields.getJSONObject(i);
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
                    if (name != null) {
                        JSONObject table = new JSONObject();
                        List listValue = new ArrayList();
                        if (value instanceof List) {
                            listValue = (List) value;
                        } else if (value instanceof Map) {
                            Map<String, Object> map = (Map) value;
                            listValue.add(new JSONObject(map));
                            recursionForTable(table, translationMap, alertMap, key, listValue, key, fieldObj);
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
                                recursionForTable(table, translationMap, alertMap, key, listValue, key, fieldObj);
                                tableList.add(table);
                            }
                        } else {
                            lineList.add(new JSONObject() {
                                {
                                    this.put("key", name);
                                    this.put("value", "暂无数据");
                                }
                            });
                        }
                    }
                } else {
                    if (value == null || Objects.equals(StringUtils.EMPTY, value)) {
                        value = "暂无数据";
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
    private void recursionForTranslation(Map<String, String> translationMap, String name, JSONArray subset) {
        if (CollectionUtils.isNotEmpty(subset)) {
            for (int i = 0; i < subset.size(); i++) {
                JSONObject _obj = subset.getJSONObject(i);
                String _name = _obj.getString("name");
                String _desc = _obj.getString("desc");
                translationMap.put(name + "." + _name, _desc);
                recursionForTranslation(translationMap, name + "." + _name, _obj.getJSONArray("subset"));
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
    private void recursionForTable(JSONObject table, Map<String, String> translationMap, Map<String, String> alertMap, String key, List array, String alertKey, JSONObject fieldObj) {
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
            Map object = (Map) array.get(i);
            JSONObject row = new JSONObject();
            int j = 0;
            for (String head : headSet) {
                Object obj = object.get(head);
                if (obj != null) {
                    if (obj instanceof List && ((List) obj).get(0) instanceof Map) {
                        List _array = (List) obj;
                        if (CollectionUtils.isNotEmpty(_array)) {
                            JSONObject _table = new JSONObject();
                            recursionForTable(_table, translationMap, alertMap, key + "." + head, _array, (alertKey + "[" + i + "]" + "." + head), fieldObj);
                            _table.remove("key");
                            row.put(headList.get(j), _table);
                        } else {
                            row.put(headList.get(j), "暂无数据");
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
                        String value = !Objects.equals(obj.toString(), StringUtils.EMPTY) ? obj.toString() : "暂无数据";
                        if (alertLevel != null) {
                            value += ("&=&" + alertLevel); // 如果有告警，则拼接告警级别到末尾，freemarker解析时，按&=&分割正文与告警级别，根据告警级别确定正文的样式
                        }
                        row.put(headList.get(j), value);
                    }
                } else {
                    row.put(headList.get(j), "暂无数据");
                }
                j++;
            }
            valueList.add(row);
        }
    }

}

/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.DocType;
import codedriver.framework.util.ExportUtil;
import codedriver.framework.util.FreemarkerUtil;
import codedriver.framework.util.TimeUtil;
import codedriver.module.inspect.service.InspectReportService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import java.io.*;
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
    private ResourceCenterMapper resourceCenterMapper;

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
            @Param(name = "type", type = ApiParamType.ENUM, rule = "word,pdf", desc = "类型", isRequired = true)
    })
    @Description(desc = "导出巡检报告")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long resourceId = paramObj.getLong("resourceId");
        String id = paramObj.getString("id");
        String type = paramObj.getString("type");
        ResourceVo resource = resourceCenterMapper.getResourceById(resourceId, TenantContext.get().getDataDbName());
        String fileName = resourceId.toString();
        if (resource != null && resource.getName() != null) {
            fileName = resource.getName();
        }
        Document reportDoc = inspectReportService.getInspectReport(resourceId, id);
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
            JSONArray lineList = new JSONArray();
            JSONArray tableList = new JSONArray();

            getDataMap(reportDoc, translationMap, lineList, tableList);
            JSONObject dataObj = new JSONObject();
            dataObj.put("lineList", lineList);
            dataObj.put("tableList", tableList);
            String content = FreemarkerUtil.transform(dataObj, template);
            try (OutputStream os = response.getOutputStream()) {
                if (DocType.WORD.getValue().equals(type)) {
                    response.setContentType("application/x-download");
                    response.setHeader("Content-Disposition",
                            " attachment; filename=\"" + URLEncoder.encode(fileName + "_巡检报告", StandardCharsets.UTF_8.name()) + ".docx\"");
                    ExportUtil.getWordFileByHtml(content, os, true, false);
                } else if (DocType.PDF.getValue().equals(type)) {
                    response.setContentType("application/pdf");
                    response.setHeader("Content-Disposition",
                            " attachment; filename=\"" + URLEncoder.encode(fileName + "_巡检报告", StandardCharsets.UTF_8.name()) + ".pdf\"");
                    ExportUtil.getPdfFileByHtml(content, os, true, true);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        return null;
    }

    /**
     * 解析MongoDB Document
     *
     * @param reportJson     待解析的document
     * @param translationMap 译文
     * @param lineList       存储String、int或Array字段的list
     * @param tableList      存储JsonArray字段的list
     */
    private void getDataMap(Map<String, Object> reportJson, Map<String, String> translationMap, JSONArray lineList, JSONArray tableList) {
        Map<String, List> tableMap = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : reportJson.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String name = translationMap.get(key);
            if (name != null) {
                if (value instanceof List) {
                    tableMap.put(key, (List) value);
                } else {
                    if (value == null) {
                        value = "暂无数据";
                    }
                    if (value instanceof Date) {
                        value = TimeUtil.convertDateToString((Date) value, TimeUtil.YYYY_MM_DD_HH_MM_SS);
                    }
                    JSONObject line = new JSONObject();
                    line.put("key", name);
                    line.put("value", value.toString());
                    lineList.add(line);
                }
            }
        }
        if (!tableMap.isEmpty()) {
            for (Map.Entry<String, List> entry : tableMap.entrySet()) {
                String name = translationMap.get(entry.getKey());
                if (name != null) {
                    JSONObject table = new JSONObject();
                    List value = entry.getValue();
                    if (CollectionUtils.isNotEmpty(value)) {
                        if (!(value.get(0) instanceof Document)) { // 元素类型不是Document，说明value是非JSONObject数组
                            lineList.add(new JSONObject() {
                                {
                                    this.put("key", name);
                                    this.put("value", value.toString());
                                }
                            });
                        } else {
                            recursionForTable(table, translationMap, entry.getKey(), value);
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
     */
    private void recursionForTable(JSONObject table, Map<String, String> translationMap, String key, List array) {
        Set<String> headSet = new LinkedHashSet<>();
        for (int i = 0; i < array.size(); i++) {
            Map map = (Map) array.get(i);
            headSet.addAll(map.keySet());
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
                    if (obj instanceof List) {
                        List _array = (List) obj;
                        if (CollectionUtils.isNotEmpty(_array)) {
                            JSONObject _table = new JSONObject();
                            recursionForTable(_table, translationMap, key + "." + head, _array);
                            _table.remove("key");
                            row.put(headList.get(j), _table);
                        } else {
                            row.put(headList.get(j), "暂无数据");
                        }
                    } else {
                        if (obj instanceof Date) {
                            obj = TimeUtil.convertDateToString((Date) obj, TimeUtil.YYYY_MM_DD_HH_MM_SS);
                        }
                        row.put(headList.get(j), StringUtils.isNotBlank(obj.toString()) ? obj.toString() : "暂无数据");
                    }
                } else {
                    row.put(headList.get(j), StringUtils.isNotBlank(obj.toString()) ? obj.toString() : "暂无数据");
                }
                j++;
            }
            valueList.add(row);
        }
    }

}

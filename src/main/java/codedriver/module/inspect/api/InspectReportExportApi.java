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
import codedriver.framework.util.TimeUtil;
import codedriver.module.inspect.service.InspectReportService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Service
public class InspectReportExportApi extends PrivateBinaryStreamApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(InspectReportExportApi.class);

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
            try (OutputStream os = response.getOutputStream()) {
                if (DocType.WORD.getValue().equals(type)) {
                    response.setContentType("application/x-download");
                    response.setHeader("Content-Disposition",
                            " attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()) + ".docx\"");
                    ExportUtil.getWordFileByHtml(getHtmlContent(reportDoc, translationMap), os, true, false);
                } else if (DocType.PDF.getValue().equals(type)) {
                    response.setContentType("application/pdf");
                    response.setHeader("Content-Disposition",
                            " attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()) + ".pdf\"");
                    ExportUtil.getPdfFileByHtml(getHtmlContent(reportDoc, translationMap), os, true, true);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        return null;
    }

    private String getHtmlContent(Map<String, Object> reportJson, Map<String, String> translationMap) throws Exception {
        StringWriter out = new StringWriter();
        out.write("<html xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\" xmlns:x=\"urn:schemas-microsoft-com:office:excel\" xmlns=\"http://www.w3.org/TR/REC-html40\">\n");
        out.write("<head>\n");
        out.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></meta>\n");
        out.write("<style></style>\n");
        out.write("</head>\n");
        out.write("<body>\n");
        StringBuilder sb = new StringBuilder();
        sb.append("<table>");
        sb.append("<tbody>");
        Map<String, List> map = new LinkedHashMap<>();
        int i = 1;
        // 渲染非JSONArray数据
        for (Map.Entry<String, Object> entry : reportJson.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String name = translationMap.get(key);
            if (name != null) {
                if (value instanceof List) {
                    map.put(key, (List) value);
                } else if (i % 2 != 0) {
                    if (i != 1) {
                        sb.append("</tr>");
                    }
                    if (value == null) {
                        value = "暂无数据";
                    }
                    if (value instanceof Date) {
                        value = TimeUtil.convertDateToString((Date) value, TimeUtil.YYYY_MM_DD_HH_MM_SS);
                    }
                    sb.append("<tr>");
                    sb.append("<td>").append(name).append("</td>");
                    sb.append("<td>").append(value.toString()).append("</td>");
                    i++;
                } else {
                    if (value == null) {
                        value = "暂无数据";
                    }
                    if (value instanceof Date) {
                        value = TimeUtil.convertDateToString((Date) value, TimeUtil.YYYY_MM_DD_HH_MM_SS);
                    }
                    sb.append("<td>").append(name).append("</td>");
                    sb.append("<td>").append(value.toString()).append("</td>");
                    i++;
                }
            }
        }
        if (sb.lastIndexOf("</tr>") != sb.length() - 1) {
            sb.append("</tr>");
        }
        // 渲染JSONArray数据为table
        if (!map.isEmpty()) {
            for (Map.Entry<String, List> entry : map.entrySet()) {
                sb.append("<tr>");
                sb.append("<td>").append(translationMap.get(entry.getKey())).append("</td>");
                if (CollectionUtils.isNotEmpty(entry.getValue())) {
                    sb.append("<td>");
                    recursionForTable(sb, translationMap, entry.getKey(), entry.getValue());
                    sb.append("</td>");
                }
                sb.append("</tr>");
            }
        }

        sb.append("</tbody>");
        sb.append("</table>");
        out.write(sb.toString());

        out.write("\n</body>\n</html>");
        out.flush();
        out.close();
        return out.toString();
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
     * 将JSONArray结构的字段渲染成Table
     *
     * @param sb
     * @param translationMap
     * @param key
     * @param array
     */
    private void recursionForTable(StringBuilder sb, Map<String, String> translationMap, String key, List array) {
        sb.append("<table style=\"" + tableStyle + "\">");
        Set<String> headSet = new LinkedHashSet<>();
        for (int i = 0; i < array.size(); i++) {
            Map map = (Map) array.get(i);
            headSet.addAll(map.keySet());
        }
        sb.append("<thead><tr>");
        Iterator<String> iterator = headSet.iterator();
        while (iterator.hasNext()) {
            String name = translationMap.get(key + "." + iterator.next());
            if (name != null) {
                sb.append("<th style=\"" + tdStyle + "\">").append(name).append("</th>");
            } else {
                iterator.remove(); // 抛弃没有译文的字段
            }
        }
        sb.append("</tr></thead>");
        sb.append("<tbody>");

        for (int i = 0; i < array.size(); i++) {
            Map object = (Map) array.get(i);
            sb.append("<tr>");
            for (String head : headSet) {
                Object obj = object.get(head);
                if (obj != null) {
                    if (obj instanceof List) {
                        List _array = (List) obj;
                        if (CollectionUtils.isNotEmpty(_array)) {
                            recursionForTable(sb, translationMap, key + "." + head, _array);
                        }
                    } else {
                        if (obj instanceof Date) {
                            obj = TimeUtil.convertDateToString((Date) obj, TimeUtil.YYYY_MM_DD_HH_MM_SS);
                        }
                        sb.append("<td style=\"" + tdStyle + "\">").append(obj.toString()).append("</td>");
                    }
                } else {
                    sb.append("<td style=\"" + tdStyle + "\">").append(StringUtils.EMPTY).append("</td>");
                }
            }
            sb.append("</tr>");
        }
        sb.append("</tbody>");
        sb.append("</table>");
    }

    static final String tableStyle = "width:100%; text-align:center; border-collapse:collapse;";
    static final String tdStyle = "border:1px solid;";


}

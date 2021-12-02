/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.module.inspect.service.InspectReportService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.bson.Document;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Service
public class InspectReportExportApi extends PrivateBinaryStreamApiComponentBase {

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
            @Param(name = "id", type = ApiParamType.STRING, desc = "id")
    })
    @Description(desc = "导出巡检报告")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long resourceId = paramObj.getLong("resourceId");
        String id = paramObj.getString("id");
        Document reportDoc = inspectReportService.getInspectReport(resourceId, id);
        if (MapUtils.isNotEmpty(reportDoc)) {
            Map<String, String> translationMap = new HashMap<>();
            JSONObject reportJson = JSONObject.parseObject(reportDoc.toJson());
            JSONArray fields = reportJson.getJSONArray("fields");
            if (CollectionUtils.isNotEmpty(fields)) {
                for (int i = 0; i < fields.size(); i++) {
                    JSONObject obj = fields.getJSONObject(i);
                    String name = obj.getString("name");
                    String desc = obj.getString("desc");
                    translationMap.put(name, desc);
                    recursion(translationMap, name, obj.getJSONArray("subset"));
                }
            }
            getHtmlContent(reportJson, translationMap);

            System.out.println(translationMap);
            Object o = JSON.toJSON(translationMap);
            System.out.println(o);
        }

        return null;
    }

    private String getHtmlContent(JSONObject reportJson, Map<String, String> translationMap) throws Exception {
        StringWriter out = new StringWriter();
        out.write("<html xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\" xmlns:x=\"urn:schemas-microsoft-com:office:excel\" xmlns=\"http://www.w3.org/TR/REC-html40\">\n");
        out.write("<head>\n");
        out.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></meta>\n");
        out.write("<style>\n" + style + "\n</style>\n");
        out.write("</head>\n");
        out.write("<body>\n");
        StringBuilder sb = new StringBuilder();
        sb.append("<table style=\"" + tableStyle + "\">");
        sb.append("<tbody>");
        int i = 1;
        for (Map.Entry<String, Object> entry : reportJson.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            String name = translationMap.get(key);
            if (name != null) {
                if (value instanceof JSONArray) { // todo 会有非对象数组吗？
                    if (i - 1 % 2 != 0) {
                        sb.append("</tr>");
                    }
                    sb.append("<tr style=\"" + trStyle + "\">");
                    sb.append("<td style=\"" + tdStyle + "\">").append(name).append("</td>");
                    JSONArray array = (JSONArray) value;
                    if (CollectionUtils.isNotEmpty(array)) {
                        sb.append("<td style=\"" + tdStyle + "\">");
                        sb.append("<table style=\"" + tableStyle + "\">");
                        Set<String> headSet = new LinkedHashSet<>();
                        for (int j = 0; j < array.size(); j++) {
                            headSet.addAll(array.getJSONObject(j).keySet());
                        }
                        // todo 注意值要和头对上
                        sb.append("<thead><tr>");
                        for (String head : headSet) {
                            String _name = translationMap.get(key + "." + head); // todo 没有中文的值应该抛弃
                            sb.append("<th>").append(_name != null ? _name : head).append("</th>");
                        }
                        sb.append("</tr></thead>");
                        sb.append("<tbody>");
                        for (int j = 0; j < array.size(); j++) {
                            JSONObject object = array.getJSONObject(j);
                            sb.append("<tr style=\"" + trStyle + "\">");
                            for (Object _value : object.values()) {
                                sb.append("<td>").append(_value).append("</td>");
                            }
                            sb.append("</tr>");
                        }
                        sb.append("</tbody>");
                        sb.append("</table>");
                        sb.append("</td>");
                    }
                    sb.append("</tr>");
                } else if (i % 2 != 0) {
                    sb.append("<tr style=\"" + trStyle + "\">");
                    sb.append("<td style=\"" + tdStyle + "\">").append(name).append("</td>");
                    sb.append("<td style=\"" + tdStyle + "\">").append(value.toString()).append("</td>");

                } else {
                    sb.append("<td style=\"" + tdStyle + "\">").append(name).append("</td>");
                    sb.append("<td style=\"" + tdStyle + "\">").append(value.toString()).append("</td>");
                    sb.append("</tr>");

                }
                i++;

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

    private void recursion(Map<String, String> translationMap, String name, JSONArray subset) {
        if (CollectionUtils.isNotEmpty(subset)) {
            for (int j = 0; j < subset.size(); j++) {
                JSONObject _obj = subset.getJSONObject(j);
                String _name = _obj.getString("name");
                String _desc = _obj.getString("desc");
                translationMap.put(name + "." + _name, _desc);
                recursion(translationMap, name + "." + _name, _obj.getJSONArray("subset"));
            }
        }
    }

    static final String tableStyle = "table-layout:fixed;border-collapse:collapse;width:100%;text-align:left;";
    static final String tdStyle = "border-bottom:1px solid grey";
    static final String trStyle = "height:42px";

    static final String style = ".tstable-container {\n" +
            "  position: relative;\n" +
            "  overflow: hidden;\n" +
            "}\n" +
            ".tstable-container.tstable-small .tstable-body th,\n" +
            ".tstable-container.tstable-small .tstable-body td {\n" +
            "  padding: 4px;\n" +
            "}\n" +
            ".tstable-container.tstable-small .tstable-body.table-top th {\n" +
            "  height: 28px;\n" +
            "}\n" +
            "\n" +
            ".tstable-container.tstable-card {\n" +
            "  border-top: 0 none;\n" +
            "}\n" +
            ".tstable-container.tstable-card .tstable-body th {\n" +
            "  border: 0 none !important;\n" +
            "}\n" +
            ".tstable-container.tstable-card .tstable-body td {\n" +
            "  opacity: 1;\n" +
            "  border: 0 none !important;\n" +
            "  position: relative;\n" +
            "  padding-top: 12px;\n" +
            "  padding-bottom: 12px;\n" +
            "}\n" +
            ".tstable-container.tstable-card .tstable-body td:before {\n" +
            "  position: absolute;\n" +
            "  content: '';\n" +
            "  top: 6px;\n" +
            "  bottom: 6px;\n" +
            "  left: 0;\n" +
            "  right: 0;\n" +
            "}\n" +
            ".tstable-container.tstable-card .tstable-body td > div {\n" +
            "  position: relative;\n" +
            "}\n" +
            ".tstable-container.tstable-card.tstable-nohover .tstable-body tr td .action-div {\n" +
            "  top: 6px;\n" +
            "  bottom: 6px;\n" +
            "}\n" +
            ".tstable-container.tstable-noborder td {\n" +
            "  border-bottom: 0 none;\n" +
            "}\n" +
            ".tstable-container:hover .btn-setting {\n" +
            "  opacity: 1;\n" +
            "}\n" +
            ".tstable-container .btn-setting {\n" +
            "  position: absolute;\n" +
            "  top: 0px;\n" +
            "  right: 0;\n" +
            "  z-index: 9;\n" +
            "}\n" +
            ".tstable-container .btn-setting .icon-setting {\n" +
            "  padding: 9px 9px;\n" +
            "  padding-right: 25px;\n" +
            "  cursor: pointer;\n" +
            "  display: block;\n" +
            "}\n" +
            ".tstable-container .tstable-main {\n" +
            "  overflow: auto;\n" +
            "  min-height: 40px;\n" +
            "}\n" +
            ".tstable-container .table-top {\n" +
            "  position: relative;\n" +
            "  z-index: 9;\n" +
            "}\n" +
            ".tstable-container .table-top > tbody > tr > td,\n" +
            ".tstable-container .table-top > tbody > tr > th {\n" +
            "  height: 0;\n" +
            "  overflow: hidden;\n" +
            "  padding-top: 0 !important;\n" +
            "  padding-bottom: 0 !important;\n" +
            "  border-bottom: 0 none !important;\n" +
            "  line-height: 0;\n" +
            "}\n" +
            ".tstable-container .table-top > tbody > tr > td > *,\n" +
            ".tstable-container .table-top > tbody > tr > th > * {\n" +
            "  height: 0 !important;\n" +
            "  overflow: hidden;\n" +
            "  margin-top: 0 !important;\n" +
            "  margin-bottom: 0 !important;\n" +
            "  border-top: 0 none !important;\n" +
            "  border-bottom: 0 none !important;\n" +
            "}\n" +
            ".tstable-container .table-main > thead > tr > th {\n" +
            "  height: 0;\n" +
            "  overflow: hidden;\n" +
            "  padding-top: 0 !important;\n" +
            "  padding-bottom: 0 !important;\n" +
            "  border-bottom: 0 none !important;\n" +
            "  line-height: 0;\n" +
            "}\n" +
            ".tstable-container .table-main > thead > tr > th > * {\n" +
            "  height: 0 !important;\n" +
            "  overflow: hidden;\n" +
            "  margin-top: 0 !important;\n" +
            "  margin-bottom: 0 !important;\n" +
            "  border-top: 0 none !important;\n" +
            "  border-bottom: 0 none !important;\n" +
            "}\n" +
            ".tstable-container .tstable-body {\n" +
            "  min-width: 100%;\n" +
            "  text-align: left;\n" +
            "  border-collapse: collapse;\n" +
            "  border-spacing: 0;\n" +
            "}\n" +
            ".tstable-container .tstable-body th,\n" +
            ".tstable-container .tstable-body td {\n" +
            "  padding: 9px;\n" +
            "  font-weight: normal;\n" +
            "  line-height: inherit;\n" +
            "}\n" +
            ".tstable-container .tstable-body th {\n" +
            "  white-space: nowrap;\n" +
            "  word-break: keep-all;\n" +
            "  height: 38px;\n" +
            "  position: relative;\n" +
            "  -webkit-backface-visibility: hidden;\n" +
            "  backface-visibility: hidden;\n" +
            "  -webkit-perspective: 1000px;\n" +
            "  -moz-perspective: 1000px;\n" +
            "  -ms-perspective: 1000px;\n" +
            "  transition: none;\n" +
            "  perspective: 1000px;\n" +
            "  will-change: transform;\n" +
            "}\n" +
            ".tstable-container .tstable-body th .btn-resize {\n" +
            "  position: absolute;\n" +
            "  top: 0;\n" +
            "  right: 0;\n" +
            "  width: 8px;\n" +
            "  height: 100%;\n" +
            "  cursor: col-resize;\n" +
            "}\n" +
            ".tstable-container .tstable-body th .btn-resize:after {\n" +
            "  content: '';\n" +
            "  position: absolute;\n" +
            "  top: 0;\n" +
            "  right: 0;\n" +
            "  width: 1px;\n" +
            "  height: 100%;\n" +
            "}\n" +
            ".tstable-container .tstable-body td {\n" +
            "  white-space: nowrap;\n" +
            "  word-break: keep-all;\n" +
            "}\n" +
            ".tstable-container .tstable-body tbody tr {\n" +
            "  transition: opacity ease 0.3s;\n" +
            "}\n" +
            "\n" +
            ".tstable-container .tstable-body .tstable-selection {\n" +
            "  width: 16px;\n" +
            "  height: 16px;\n" +
            "  display: block;\n" +
            "  margin-right: 4px;\n" +
            "  margin-left: 4px;\n" +
            "  position: relative;\n" +
            "  display: inline-block;\n" +
            "}\n" +
            ".tstable-container .tstable-body .tstable-selection:hover {\n" +
            "  cursor: pointer;\n" +
            "}\n" +
            ".tstable-container .tstable-body .tstable-selection.selected:after {\n" +
            "  content: '';\n" +
            "  width: 11px;\n" +
            "  height: 6px;\n" +
            "  position: absolute;\n" +
            "  top: 50%;\n" +
            "  left: 50%;\n" +
            "  border-radius: 2px;\n" +
            "  transform: rotate(-45deg);\n" +
            "  margin-top: -5px;\n" +
            "  margin-left: -6px;\n" +
            "}\n" +
            ".tstable-container .tstable-body .tstable-selection.disabled {\n" +
            "  opacity: 0.9;\n" +
            "}\n" +
            ".tstable-container .tstable-body .tstable-selection.disabled:hover {\n" +
            "  cursor: not-allowed;\n" +
            "}\n" +
            ".tstable-container .tstable-body .tstable-selection.some:after {\n" +
            "  content: '';\n" +
            "  width: 8px;\n" +
            "  height: 2px;\n" +
            "  position: absolute;\n" +
            "  top: 50%;\n" +
            "  left: 50%;\n" +
            "  margin-top: -1px;\n" +
            "  margin-left: -4px;\n" +
            "  border: 0 none;\n" +
            "  transform: none;\n" +
            "}\n" +
            "\n" +
            ".tableaction-container {\n" +
            "  width: 0;\n" +
            "}\n" +
            ".tableaction-container .table-dropdown {\n" +
            "  padding: 0;\n" +
            "  margin: 0;\n" +
            "  box-shadow: none;\n" +
            "  top: 0;\n" +
            "  right: 100%;\n" +
            "  display: none;\n" +
            "}\n" +
            ".tableaction-container .table-dropdown .ivu-dropdown-menu {\n" +
            "  word-break: keep-all;\n" +
            "  white-space: nowrap;\n" +
            "  display: block;\n" +
            "  min-width: auto;\n" +
            "}\n" +
            ".tableaction-container .table-dropdown .ivu-dropdown-menu .ivu-dropdown-item {\n" +
            "  display: inline-block;\n" +
            "  padding: 0 10px;\n" +
            "  position: relative;\n" +
            "}\n" +
            ".tableaction-container .table-dropdown .ivu-dropdown-menu .ivu-dropdown-item:not(:last-of-type):after {\n" +
            "  content: '';\n" +
            "  width: 1px;\n" +
            "  height: 14px;\n" +
            "  top: 50%;\n" +
            "  margin-top: -7px;\n" +
            "  right: 0;\n" +
            "  position: absolute;\n" +
            "}\n" +
            ".tableaction-container .table-dropdown .ivu-dropdown-menu .ivu-dropdown-item:hover {\n" +
            "  background: transparent;\n" +
            "}\n" +
            ".ck-content {\n" +
            "  min-height: 130px;\n" +
            "}\n" +
            "html .ck.ck-reset_all,\n" +
            "html .ck.ck-reset_all * {\n" +
            "  color: #212121;\n" +
            "}\n" +
            "html .ck.ck-button:not(.ck-disabled):hover,\n" +
            "html a.ck.ck-button:not(.ck-disabled):hover {\n" +
            "  background-color: #fff;\n" +
            "}\n" +
            "html .ck.ck-button.ck-on,\n" +
            "html a.ck.ck-button.ck-on {\n" +
            "  background-color: #fff;\n" +
            "}\n" +
            "html .ck.ck-list {\n" +
            "  background-color: #fff;\n" +
            "}\n" +
            "html .ck.ck-list__item .ck-button:hover:not(.ck-disabled) {\n" +
            "  background-color: #E7F3FF;\n" +
            "}\n" +
            "html .ck.ck-list__item .ck-button.ck-on {\n" +
            "  background: #E7F3FF;\n" +
            "  color: #00bcd4;\n" +
            "}\n" +
            "html .ck.ck-dropdown__panel {\n" +
            "  background: #fff;\n" +
            "  border-color: #DBDBDB;\n" +
            "}\n" +
            "html .ck-content .table table td,\n" +
            "html .ck-content .table table th,\n" +
            "html .ck-content .table table {\n" +
            "  border-color: #DBDBDB !important;\n" +
            "  border: 1px solid;\n" +
            "}\n" +
            "\n" +
            "section {\n" +
            "  position: relative;\n" +
            "}\n" +
            " .sheet-table {\n" +
            "  width: 100%;\n" +
            "  border-collapse: collapse;\n" +
            "  border-spacing: 0px;\n" +
            "  table-layout: fixed;\n" +
            "  outline: none;\n" +
            "}\n" +
            " .sheet-table thead {\n" +
            "  height: 0;\n" +
            "}\n" +
            " .sheet-table tbody tr td {\n" +
            "  border: 1px solid;\n" +
            "  vertical-align: middle;\n" +
            "  padding: 3px;\n" +
            "  height: 40px;\n" +
            "  position: relative;\n" +
            "  word-break: break-all;\n" +
            "}\n" +
            " .sheet-table tbody tr td.text-right {\n" +
            "  padding-right: 12px;\n" +
            "}\n" +
            ".tstable-container .tstable-body tbody tr:hover {\n" +
            "  background: transparent !important;\n" +
            "}\n" +
            ".table-color .tstable-container {\n" +
            "  overflow: auto;\n" +
            "  border-top: 0px !important;\n" +
            "}\n" +
            ".table-color .tstable-container .table-list {\n" +
            "  width: 100%;\n" +
            "  border-top: none;\n" +
            "  border-collapse: collapse;\n" +
            "  table-layout: fixed;\n" +
            "}\n" +
            ".table-color .tstable-container .table-list > thead,\n" +
            ".table-color .tstable-container .table-list > thead > tr > th {\n" +
            "  visibility: visible !important;\n" +
            "  border: none !important;\n" +
            "  vertical-align: middle;\n" +
            "  height: 38px;\n" +
            "  padding-top: 0px;\n" +
            "  padding-bottom: 0px;\n" +
            "  text-align: left;\n" +
            "}\n" +
            ".table-color .tstable-container .table-list > tbody > tr > td {\n" +
            "  border-left: none !important;\n" +
            "  border-right: none !important;\n" +
            "  border-bottom: none !important;\n" +
            "  border-top: none !important;\n" +
            "  vertical-align: top;\n" +
            "}\n" +
            "\n" +
            ".ck-content .table table td,\n" +
            ".ck-content .table table th,\n" +
            ".ck-content .table table {\n" +
            "  border-color: #DBDBDB !important;\n" +
            "  border: 1px solid;\n" +
            "}\n" +
            "\n" +
            "ul li {\n" +
            "  list-style: disc;\n" +
            "}\n" +
            " ol {\n" +
            "  list-style: decimal inside;\n" +
            "}\n" +
            " ol li {\n" +
            "  list-style: decimal;\n" +
            "}\n" +
            "ol.cjk-ideographic li {\n" +
            "  list-style: cjk-ideographic;\n" +
            "}\n" +
            "span.line-through {\n" +
            "  text-decoration: line-through;\n" +
            "  vertical-align: baseline;\n" +
            "}\n" +
            "[class=line-through] * {\n" +
            "  text-decoration: line-through;\n" +
            "  vertical-align: baseline;\n" +
            "}\n" +
            "body{\n" +
            "  font-size:14px;\n" +
            "}\n" +
            "h2{\n" +
            "  font-size:14px;\n" +
            "}\n" +
            "h1{\n" +
            "  font-size:16px;\n" +
            "}";


}

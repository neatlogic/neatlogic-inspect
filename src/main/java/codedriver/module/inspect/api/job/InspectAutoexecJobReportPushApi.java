/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api.job;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.cmdb.crossover.ICiCrossoverMapper;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.BgVo;
import codedriver.framework.cmdb.dto.resourcecenter.IpVo;
import codedriver.framework.cmdb.dto.resourcecenter.OwnerVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.common.config.Config;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.dao.mapper.RoleMapper;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.RoleVo;
import codedriver.framework.dto.TeamVo;
import codedriver.framework.dto.UserVo;
import codedriver.framework.inspect.auth.INSPECT_EXECUTE;
import codedriver.framework.inspect.auth.INSPECT_SCHEDULE_EXECUTE;
import codedriver.framework.inspect.dto.InspectConfigFilePathVo;
import codedriver.framework.inspect.dto.InspectResourceSearchVo;
import codedriver.framework.inspect.dto.InspectResourceVo;
import codedriver.framework.notify.core.INotifyHandler;
import codedriver.framework.notify.core.NotifyHandlerFactory;
import codedriver.framework.notify.dto.NotifyVo;
import codedriver.framework.notify.exception.NotifyHandlerNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TimeUtil;
import codedriver.module.inspect.dao.mapper.InspectConfigFileMapper;
import codedriver.module.inspect.service.InspectReportService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = INSPECT_SCHEDULE_EXECUTE.class)
@AuthAction(action = INSPECT_EXECUTE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class InspectAutoexecJobReportPushApi extends PrivateApiComponentBase {

    @Resource
    MongoTemplate mongoTemplate;

    @Resource
    private InspectConfigFileMapper inspectConfigFileMapper;

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    InspectReportService inspectReportService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private TeamMapper teamMapper;

    @Resource
    private RoleMapper roleMapper;

    @Override
    public String getToken() {
        return "inspect/autoexec/job/report/push";
    }

    @Override
    public String getName() {
        return "推送巡检报告";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "notifyHandler", type = ApiParamType.ENUM, rule = "emailNotifyHandler", isRequired = true, desc = "推送方式"),
            @Param(name = "receiverList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "推送对象"),
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "作业ID")
    })
    @Output({

    })
    @Description(desc = "推送巡检报告")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String notifyHandler = paramObj.getString("notifyHandler");
        INotifyHandler handler = NotifyHandlerFactory.getHandler(notifyHandler);
        if (handler == null) {
            throw new NotifyHandlerNotFoundException(notifyHandler);
        }
        Long jobId = paramObj.getLong("jobId");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        NotifyVo.Builder notifyBuilder = new NotifyVo.Builder();
        boolean needSend = false;
        // 文件变更记录
        JSONObject fileChangeAuditTable = getFileChangeAuditTable(jobId);
        if (fileChangeAuditTable != null) {
            notifyBuilder.addData("fileChangeAuditTable", fileChangeAuditTable);
            Integer fileChangeCount = fileChangeAuditTable.getInteger("fileChangeCount");
            notifyBuilder.addData("fileChangeCount", fileChangeCount);
            needSend = true;
        }
        // 问题报告
        JSONObject problemReportTable = getProblemReportTable(jobId);
        if (problemReportTable != null) {
            notifyBuilder.addData("problemReportTable", problemReportTable);
            Integer resourceCount = problemReportTable.getInteger("resourceCount");
            notifyBuilder.addData("resourceCount", resourceCount);
            Integer problemCount = problemReportTable.getInteger("problemCount");
            notifyBuilder.addData("problemCount", problemCount);
            needSend = true;
        }
        if (!needSend) {
            return null;
        }
        notifyBuilder.addData("name", jobVo.getName());
        notifyBuilder.addData("startTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(jobVo.getStartTime()));
        String execUser = jobVo.getExecUser();
        String type = jobVo.getExecUserType();
        if (StringUtils.isNotBlank(execUser) && StringUtils.isNotBlank(type)) {
            if ("user".equals(type)) {
                UserVo userVo = userMapper.getUserBaseInfoByUuid(execUser);
                if (userVo != null) {
                    notifyBuilder.addData("execUser", userVo.getUserName());
                }
            } else if ("team".equals(type)) {
                TeamVo teamVo = teamMapper.getTeamByUuid(execUser);
                if (teamVo != null) {
                    notifyBuilder.addData("execUser", teamVo.getName());
                }
            } else if ("role".equals(type)) {
                RoleVo roleVo = roleMapper.getRoleSimpleInfoByUuid(execUser);
                if (roleVo != null) {
                    notifyBuilder.addData("execUser", roleVo.getName());
                }
            }
        }
        /** 接收人 **/
        JSONArray receiverArray = paramObj.getJSONArray("receiverList");
        for (String receiver : receiverArray.toJavaList(String.class)) {
            String[] split = receiver.split("#");
            if (GroupSearch.USER.getValue().equals(split[0])) {
                notifyBuilder.addUserUuid(split[1]);
            } else if (GroupSearch.TEAM.getValue().equals(split[0])) {
                notifyBuilder.addTeamUuid(split[1]);
            } else if (GroupSearch.ROLE.getValue().equals(split[0])) {
                notifyBuilder.addRoleUuid(split[1]);
            }
        }
        notifyBuilder.withTitleTemplate(getTitleTemplate());
        notifyBuilder.withContentTemplate(getContentTemplate());
        NotifyVo notifyVo = notifyBuilder.build();
        handler.execute(notifyVo);
        return null;
    }

    /**
     * 文件变更记录表格数据
     * @param jobId 作业ID
     * @return 表格数据
     */
    private JSONObject getFileChangeAuditTable(Long jobId) {
        List<InspectConfigFilePathVo> inspectResourceConfigFilePathList = inspectConfigFileMapper.getInspectConfigFilePathListByJobId(jobId);
        if (CollectionUtils.isEmpty(inspectResourceConfigFilePathList)) {
            return null;
        }
        JSONArray tbodyList = new JSONArray();
        for (InspectConfigFilePathVo inspectConfigFilePathVo : inspectResourceConfigFilePathList) {
            Map<String, String> tbody = new HashMap<>();
            String path = inspectConfigFilePathVo.getPath();
            String homeUrl = Config.HOME_URL();
            if(StringUtils.isNotBlank(homeUrl)) {
                StringBuilder stringBuilder = new StringBuilder(homeUrl);
                if(!homeUrl.endsWith(File.separator)) {
                    stringBuilder.append(File.separator);
                }
                stringBuilder.append(TenantContext.get().getTenantUuid());
                stringBuilder.append(File.separator);
                stringBuilder.append("inspect.html#/configfile-detail?id=");
                Long pathId = inspectConfigFilePathVo.getId();
                stringBuilder.append(pathId);
                stringBuilder.append("&resourceId=");
                stringBuilder.append(inspectConfigFilePathVo.getResourceId());
                Long versionId = inspectConfigFilePathVo.getVersionId();
                Long previousVersionId = inspectConfigFileMapper.getPreviousVersionIdByPathIdAndVersionId(pathId, versionId);
                if (previousVersionId != null) {
                    stringBuilder.append("&oldVersionId=");
                    stringBuilder.append(previousVersionId);
                    stringBuilder.append("&newVersionId=");
                    stringBuilder.append(versionId);
                    stringBuilder.append("&isShowComparison=true");
                }
                path = "<a href='" + stringBuilder.toString() + "'>" + path + "</a>";
            }
            tbody.put("path", path);
            String resourceIp = inspectConfigFilePathVo.getResourceIp();
            if (inspectConfigFilePathVo.getResourcePort() != null) {
                resourceIp += ":" + inspectConfigFilePathVo.getResourcePort();
            }
            tbody.put("resourceIp", resourceIp);
            tbody.put("resourceName", inspectConfigFilePathVo.getResourceName());
            tbody.put("resourceTypeLabel", inspectConfigFilePathVo.getResourceTypeLabel());
            tbodyList.add(tbody);
        }
        JSONObject tableData = new JSONObject();
        JSONArray theadList = getFileChangeTheadList();
        tableData.put("theadList", theadList);
        tableData.put("tbodyList", tbodyList);
        tableData.put("fileChangeCount", tbodyList.size());
        return tableData;
    }

    private JSONArray getFileChangeTheadList() {
        JSONArray theadList = new JSONArray();
        {
            JSONObject thead = new JSONObject();
            thead.put("title", "文件名");
            thead.put("key", "path");
            theadList.add(thead);
        }
        {
            JSONObject thead = new JSONObject();
            thead.put("title", "资产IP");
            thead.put("key", "resourceIp");
            theadList.add(thead);
        }
        {
            JSONObject thead = new JSONObject();
            thead.put("title", "资产名称");
            thead.put("key", "resourceName");
            theadList.add(thead);
        }
        {
            JSONObject thead = new JSONObject();
            thead.put("title", "资产类型");
            thead.put("key", "resourceTypeLabel");
            theadList.add(thead);
        }
        return theadList;
    }
    /**
     * 文件变更记录表格数据
     * @param jobId 作业ID
     * @return 表格数据
     */
    private JSONObject getProblemReportTable(Long jobId) {
        List<InspectResourceVo> inspectResourceList = getInspectResourceList(jobId);
        if (CollectionUtils.isNotEmpty(inspectResourceList)) {
            int problemCount = 0;
            JSONArray tbodyList = new JSONArray();
            Map<Long, InspectResourceVo> inspectResourceMap = inspectResourceList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
            List<Long> resourceIdList = inspectResourceList.stream().map(InspectResourceVo::getId).collect(Collectors.toList());
            JSONArray inspectReportList = getInspectReportList(jobId, resourceIdList);
            for (int i = 0; i < inspectReportList.size(); i++) {
                JSONObject inspectReport = inspectReportList.getJSONObject(i);
                if (MapUtils.isEmpty(inspectReport)) {
                    continue;
                }
                JSONObject inspectResult = inspectReport.getJSONObject("inspectResult");
                if (MapUtils.isEmpty(inspectResult)) {
                    continue;
                }
                JSONObject thresholds = inspectResult.getJSONObject("thresholds");
                if (MapUtils.isEmpty(thresholds)) {
                    continue;
                }
                JSONArray reportResultTbodyList = new JSONArray();
                for (Map.Entry<String, Object> entry : thresholds.entrySet()) {
                    reportResultTbodyList.add(entry.getValue());
                }
                problemCount += reportResultTbodyList.size();
                Long id = inspectReport.getLong("id");
                InspectResourceVo inspectResourceVo = inspectResourceMap.get(id);
                if (inspectResourceVo != null) {
                    JSONObject object = new JSONObject();
                    object.put("inspectResource", inspectResourceToMap(inspectResourceVo, jobId));
                    JSONObject reportResultTable = new JSONObject();
                    reportResultTable.put("theadList", getReportTheadList());
                    reportResultTable.put("tbodyList", reportResultTbodyList);
                    object.put("reportResultTable", reportResultTable);
                    tbodyList.add(object);
                }
            }
            if (CollectionUtils.isNotEmpty(tbodyList)) {
                JSONObject problemReportTable = new JSONObject();
                problemReportTable.put("theadList", getResourceTheadList());
                problemReportTable.put("tbodyList", tbodyList);
                problemReportTable.put("resourceCount", tbodyList.size());
                problemReportTable.put("problemCount", problemCount);
                return problemReportTable;
            }
        }
        return null;
    }
    private List<InspectResourceVo> getInspectResourceList(Long jobId) {
        List<InspectResourceVo> resultList = new ArrayList<>();
        InspectResourceSearchVo searchVo = new InspectResourceSearchVo();
        searchVo.setJobId(jobId);
        searchVo.setNeedPage(false);
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo ciVo = ciCrossoverMapper.getCiByName("IPObject");
        if (ciVo == null) {
            throw new CiNotFoundException("IPObject");
        }
        searchVo.setLft(ciVo.getLft());
        searchVo.setRht(ciVo.getRht());
        int currentPage = 1;
        while(true && currentPage < 100) {
            searchVo.setCurrentPage(currentPage);
            List<InspectResourceVo> inspectResourceList = inspectReportService.getInspectAutoexecJobNodeList(jobId, searchVo);
            resultList.addAll(inspectResourceList);
            if (searchVo.getPageCount() > currentPage) {
                currentPage++;
            } else {
                break;
            }
        }
        return resultList;
    }

    private JSONArray getInspectReportList(Long jobId, List<Long> resourceIdList) {
        JSONArray inspectReportArray = new JSONArray();
        MongoCollection<Document> collection = mongoTemplate.getCollection("INSPECT_REPORTS_HIS");
        Document doc = new Document();
        doc.put("_jobid", jobId.toString());
        for (Long id : resourceIdList) {
            JSONObject inspectReport = new JSONObject();
            inspectReport.put("id", id);
            inspectReport.put("inspectResult", new JSONObject());
            doc.put("RESOURCE_ID", id);
            FindIterable<Document> findIterable = collection.find(doc);
            Document reportDoc = findIterable.first();
            if (MapUtils.isNotEmpty(reportDoc)) {
                JSONObject reportJson = JSONObject.parseObject(reportDoc.toJson());
                JSONObject inspectResult = reportJson.getJSONObject("_inspect_result");
                if (inspectResult != null) {
                    inspectReport.put("inspectResult", inspectResult);
                }
            }
            inspectReportArray.add(inspectReport);
        }
        return inspectReportArray;
    }

    private String getTitleTemplate() {
        return "巡检作业[${DATA.name}--${DATA.startTime}--${DATA.execUser}]执行结果";
    }

    private String getContentTemplate() {
        String str = "<style>\n" +
                "\ttable{\n" +
                "\t\tborder-spacing: 0;border: 1px solid #CED2CC; border-collapse:collapse;\n" +
                "\t}\n" +
                "\t.label{\n" +
                "\t\tborder-right: 1px solid #CED2CC;font-size: 18px;margin: 0;overflow: visible;padding: .1em .1em;border-bottom: 1px solid #CED2CC;font-weight: bold; width:100px; background: #F5F7F4; \n" +
                "    }\n" +
                "    .value{\n" +
                "        font-size: 18px; margin: 0;overflow: visible;padding: .1em .1em;border-bottom: 1px solid #CED2CC;\n" +
                "    }\n" +
                "</style>\n" +
                "<#if DATA.fileChangeAuditTable??>\n" +
                "<b>文件变更记录（共${DATA.fileChangeCount}个文件变更）</b>\n" +
                "\t<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\" style=\"border-color: #000; border-collapse: collapse; font-size: 14px;width:100%;text-align:center;\">\n" +
                "\t\t<#assign theadList = DATA.fileChangeAuditTable.theadList/>\n" +
                "\t\t<#if theadList?? && theadList?size gt 0>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t<#list theadList as thead>\n" +
                "\t\t\t\t<th>${thead.title}</th>\n" +
                "\t\t\t</#list>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</#if>\n" +
                "\t\t<#assign tbodyList = DATA.fileChangeAuditTable.tbodyList/>\n" +
                "\t\t<#if tbodyList?? && tbodyList?size gt 0>\n" +
                "\t\t\t<#list tbodyList as tbody>\n" +
                "\t\t\t\t<tr>\n" +
                "\t\t\t\t\t<#list theadList as thead>\n" +
                "\t\t\t\t\t\t<#assign colKey = thead.key/>\n" +
                "\t\t\t\t\t\t<#assign cell = tbody[colKey]/>\n" +
                "\t\t\t\t\t\t<td>\n" +
                "\t\t\t\t\t\t\t${cell}\n" +
                "\t\t\t\t\t\t</td>\n" +
                "\t\t\t\t\t</#list>\n" +
                "\t\t\t\t</tr>\n" +
                "\t\t\t</#list>\n" +
                "\t\t</#if>\n" +
                "\t</table>\n" +
                "</#if>\n" +
                "<br>\n" +
                "<#if DATA.problemReportTable??>\n" +
                "<b>问题报告（共${DATA.resourceCount}个资产，${DATA.problemCount}条告警）</b>\n" +
                "\t<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\" style=\"border-color: #000; border-collapse: collapse; font-size: 14px;width:100%;text-align:center;\">\n" +
                "\t\t<#assign theadList = DATA.problemReportTable.theadList/>\n" +
                "\t\t<#if theadList?? && theadList?size gt 0>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t<#list theadList as thead>\n" +
                "\t\t\t\t<th>${thead.title}</th>\n" +
                "\t\t\t</#list>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</#if>\n" +
                "\t\t<#assign tbodyList = DATA.problemReportTable.tbodyList/>\n" +
                "\t\t<#if tbodyList?? && tbodyList?size gt 0>\n" +
                "\t\t\t<#list tbodyList as tbody>\n" +
                "\t\t\t\t<tr>\n" +
                "\t\t\t\t\t<#assign inspectResource = tbody.inspectResource/>\n" +
                "\t\t\t\t\t<#list theadList as thead>\n" +
                "\t\t\t\t\t\t<#assign colKey = thead.key/>\n" +
                "\t\t\t\t\t\t<#assign cell = inspectResource[colKey]/>\n" +
                "\t\t\t\t\t\t<td>\n" +
                "\t\t\t\t\t\t\t${cell}\n" +
                "\t\t\t\t\t\t</td>\n" +
                "\t\t\t\t\t</#list>\n" +
                "\t\t\t\t</tr>\n" +
                "\t\t\t\t<#if tbody.reportResultTable??>\n" +
                "\t\t\t\t<tr>\n" +
                "\t\t\t\t\t<td colspan=\"13\">\n" +
                "\t\t\t\t\t\t<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\" style=\"border-color: #000; border-collapse: collapse; font-size: 14px;width:100%;text-align:center;\">\n" +
                "\t\t\t\t\t\t<#assign reportResultTheadList = tbody.reportResultTable.theadList/>\n" +
                "\t\t\t\t\t\t<#if reportResultTheadList?? && reportResultTheadList?size gt 0>\n" +
                "\t\t\t\t\t\t\t<tr>\n" +
                "\t\t\t\t\t\t\t<#list reportResultTheadList as thead>\n" +
                "\t\t\t\t\t\t\t\t<th>${thead.title}</th>\n" +
                "\t\t\t\t\t\t\t</#list>\n" +
                "\t\t\t\t\t\t\t</tr>\n" +
                "\t\t\t\t\t\t</#if>\n" +
                "\t\t\t\t\t\t<#assign reportResultTbodyList = tbody.reportResultTable.tbodyList/>\n" +
                "\t\t\t\t\t\t<#if reportResultTbodyList?? && reportResultTbodyList?size gt 0>\n" +
                "\t\t\t\t\t\t\t<#list reportResultTbodyList as reportResultTbody>\n" +
                "\t\t\t\t\t\t\t\t<tr>\n" +
                "\t\t\t\t\t\t\t\t\t<#list reportResultTheadList as thead>\n" +
                "\t\t\t\t\t\t\t\t\t\t<#assign colKey = thead.key/>\n" +
                "\t\t\t\t\t\t\t\t\t\t<#assign cell = reportResultTbody[colKey]/>\n" +
                "\t\t\t\t\t\t\t\t\t\t<td>\n" +
                "\t\t\t\t\t\t\t\t\t\t\t${cell}\n" +
                "\t\t\t\t\t\t\t\t\t\t</td>\n" +
                "\t\t\t\t\t\t\t\t\t</#list>\n" +
                "\t\t\t\t\t\t\t\t</tr>\n" +
                "\t\t\t\t\t\t\t</#list>\n" +
                "\t\t\t\t\t\t\t</#if>\n" +
                "\t\t\t\t\t\t</table>\n" +
                "\t\t\t\t\t</td>\n" +
                "\t\t\t\t</tr>\n" +
                "\t\t\t\t</#if>\n" +
                "\t\t\t</#list>\n" +
                "\t\t</#if>\n" +
                "\t</table>\n" +
                "</#if>";
        return str;
    }

    private JSONArray getResourceTheadList() {
        JSONArray theadList = new JSONArray();
        {
            JSONObject thead = new JSONObject();
            thead.put("title", "IP地址");
            thead.put("key", "ip");
            theadList.add(thead);
        }
        {
            JSONObject thead = new JSONObject();
            thead.put("title", "类型");
            thead.put("key", "typeLabel");
            theadList.add(thead);
        }
        {
            JSONObject thead = new JSONObject();
            thead.put("title", "名称");
            thead.put("key", "name");
            theadList.add(thead);
        }
        {
            JSONObject thead = new JSONObject();
            thead.put("title", "描述");
            thead.put("key", "description");
            theadList.add(thead);
        }
        {
            JSONObject thead = new JSONObject();
            thead.put("title", "监控状态");
            thead.put("key", "monitorTime");
            theadList.add(thead);
        }
        {
            JSONObject thead = new JSONObject();
            thead.put("title", "巡检状态");
            thead.put("key", "inspectTime");
            theadList.add(thead);
        }
        {
            JSONObject thead = new JSONObject();
            thead.put("title", "IP列表");
            thead.put("key", "allIp");
            theadList.add(thead);
        }
        {
            JSONObject thead = new JSONObject();
            thead.put("title", "所属部门");
            thead.put("key", "bgList");
            theadList.add(thead);
        }
        {
            JSONObject thead = new JSONObject();
            thead.put("title", "所有者");
            thead.put("key", "ownerList");
            theadList.add(thead);
        }
        {
            JSONObject thead = new JSONObject();
            thead.put("title", "资产状态");
            thead.put("key", "stateName");
            theadList.add(thead);
        }
        {
            JSONObject thead = new JSONObject();
            thead.put("title", "网络区域");
            thead.put("key", "networkArea");
            theadList.add(thead);
        }
        {
            JSONObject thead = new JSONObject();
            thead.put("title", "标签");
            thead.put("key", "tagList");
            theadList.add(thead);
        }
        {
            JSONObject thead = new JSONObject();
            thead.put("title", "维护窗口");
            thead.put("key", "maintenanceWindow");
            theadList.add(thead);
        }
        return theadList;
    }

    private JSONArray getReportTheadList() {
        JSONArray theadList = new JSONArray();
        {
            JSONObject thead = new JSONObject();
            thead.put("title", "级别");
            thead.put("key", "level");
            theadList.add(thead);
        }
        {
            JSONObject thead = new JSONObject();
            thead.put("title", "告警提示");
            thead.put("key", "name");
            theadList.add(thead);
        }
        {
            JSONObject thead = new JSONObject();
            thead.put("title", "规则");
            thead.put("key", "rule");
            theadList.add(thead);
        }
        return theadList;
    }

    private Map<String, String> inspectResourceToMap(InspectResourceVo resourceVo, Long jobId) {
        Map<String, String> map = new HashMap<>();
        String ip = resourceVo.getIp();
        if (resourceVo.getPort() != null) {
            ip += ":" + resourceVo.getPort();
        }
        String homeUrl = Config.HOME_URL();
        if(StringUtils.isNotBlank(homeUrl)) {
            StringBuilder stringBuilder = new StringBuilder(homeUrl);
            if(!homeUrl.endsWith(File.separator)) {
                stringBuilder.append(File.separator);
            }
            stringBuilder.append(TenantContext.get().getTenantUuid());
            stringBuilder.append(File.separator);
            stringBuilder.append("inspect.html#/assets-detail-");
            stringBuilder.append(resourceVo.getId());
            stringBuilder.append("?jobId=");
            stringBuilder.append(jobId);
            stringBuilder.append("&backPage=questionReport");
            ip = "<a href='" + stringBuilder.toString() + "'>" + ip + "</a>";
        }
        map.put("ip", ip);
        map.put("typeLabel", resourceVo.getTypeLabel() == null ? "" : resourceVo.getTypeLabel());
        map.put("name", resourceVo.getName() == null ? "" : resourceVo.getName());
        map.put("description", resourceVo.getDescription() == null ? "" : resourceVo.getDescription());
        String monitorTimeStr = "";
        Date monitorTime = resourceVo.getMonitorTime();
        if (monitorTime != null) {
            String before = TimeUtil.millisecondsTransferMaxTimeUnit(System.currentTimeMillis() - monitorTime.getTime());
            monitorTimeStr = before + "之前";
            String monitorStatus = resourceVo.getMonitorStatus();
            if (StringUtils.isNotBlank(monitorStatus)) {
                monitorTimeStr = monitorStatus + " " + monitorTimeStr;
            }
        }
        map.put("monitorTime", monitorTimeStr);
        String inspectTimeStr = "";
        Date inspectTime = resourceVo.getInspectTime();
        if (inspectTime != null) {
            String before = TimeUtil.millisecondsTransferMaxTimeUnit(System.currentTimeMillis() - inspectTime.getTime());
            inspectTimeStr = before + "之前";
            String inspectStatus = resourceVo.getInspectStatus();
            if (StringUtils.isNotBlank(inspectStatus)) {
                inspectTimeStr = inspectStatus + " " + inspectTimeStr;
            }
        }
        map.put("inspectTime", inspectTimeStr);
        String ipListStr = "";
        List<IpVo> allIp = resourceVo.getAllIp();
        if (CollectionUtils.isNotEmpty(allIp)) {
            List<String> ipList = allIp.stream().map(IpVo::getIp).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(ipList)) {
                ipListStr = String.join(",", ipList);
            }
        }
        map.put("allIp", ipListStr);
        String bgListStr = "";
        List<BgVo> bgList = resourceVo.getBgList();
        if (CollectionUtils.isNotEmpty(bgList)) {
            List<String> bgNameList = bgList.stream().map(BgVo::getBgName).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(bgNameList)) {
                bgListStr = String.join(",", bgNameList);
            }
        }
        map.put("bgList", bgListStr);
        String ownerListStr = "";
        List<OwnerVo> ownerList = resourceVo.getOwnerList();
        if (CollectionUtils.isNotEmpty(ownerList)) {
            List<String> userNameList = ownerList.stream().map(OwnerVo::getUserName).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(userNameList)) {
                ownerListStr = String.join(",", userNameList);
            }
        }
        map.put("ownerList", ownerListStr);
        map.put("stateName", resourceVo.getStateName() == null ? "" : resourceVo.getStateName());
        map.put("networkArea", resourceVo.getNetworkArea() == null ? "" : resourceVo.getNetworkArea());
        String tagListStr = "";
        List<String> tagList = resourceVo.getTagList();
        if (CollectionUtils.isNotEmpty(tagList)) {
            tagListStr = String.join(",", tagListStr);
        }
        map.put("tagList", tagListStr);
        map.put("maintenanceWindow", resourceVo.getMaintenanceWindow() == null ? "" : resourceVo.getMaintenanceWindow());
        return map;
    }
}

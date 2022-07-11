package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.cmdb.crossover.IResourceCenterCommonGenerateSqlCrossoverService;
import codedriver.framework.cmdb.crossover.IResourceCenterCustomGenerateSqlCrossoverService;
import codedriver.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import codedriver.framework.cmdb.dto.cientity.CiEntityInspectVo;
import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceInfo;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.cmdb.utils.ResourceSearchGenerateSqlUtil;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dao.mapper.InspectMapper;
import codedriver.framework.inspect.dto.InspectResourceScriptVo;
import codedriver.framework.inspect.dto.InspectResourceSearchVo;
import codedriver.framework.inspect.dto.InspectResourceVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.inspect.service.InspectReportService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/2/22 5:57 下午
 */
@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class InspectAutoexecJobNodeSearchApi extends PrivateApiComponentBase {

    @Resource
    InspectReportService inspectReportService;

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    InspectMapper inspectMapper;

    @Override
    public String getName() {
        return "查询巡检作业节点资产";
    }

    @Override
    public String getToken() {
        return "inspect/autoexec/job/node/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "模糊搜索"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true, desc = "类型id"),
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "类型id"),
            @Param(name = "protocolIdList", type = ApiParamType.JSONARRAY, desc = "协议id列表"),
            @Param(name = "stateIdList", type = ApiParamType.JSONARRAY, desc = "状态id列表"),
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, desc = "环境id列表"),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "应用系统id列表"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "资源类型id列表"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, desc = "标签id列表"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "巡检状态列表"),
            @Param(name = "inspectJobPhaseNodeStatusList", type = ApiParamType.JSONARRAY, desc = "巡检作业状态列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = InspectResourceVo[].class, desc = "巡检作业节点资产列表")
    })
    @Description(desc = "巡检作业节点资产查询接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
//        long startTime = System.currentTimeMillis();
        InspectResourceSearchVo searchVo = JSON.toJavaObject(paramObj, InspectResourceSearchVo.class);
        Long jobId = searchVo.getJobId();
//        System.out.println("jobId=" + jobId);
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
//        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
//        CiVo ciVo = ciCrossoverMapper.getCiById(searchVo.getTypeId());
//        if (ciVo == null) {
//            throw new CiNotFoundException(searchVo.getTypeId());
//        }
//        searchVo.setLft(ciVo.getLft());
//        searchVo.setRht(ciVo.getRht());
//        return TableResultUtil.getResult(inspectReportService.getInspectAutoexecJobNodeList(jobId, searchVo), searchVo);

        IResourceCenterResourceCrossoverService resourceCenterResourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
        IResourceCenterCommonGenerateSqlCrossoverService resourceCenterCommonGenerateSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterCommonGenerateSqlCrossoverService.class);
        IResourceCenterCustomGenerateSqlCrossoverService resourceCenterCustomGenerateSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterCustomGenerateSqlCrossoverService.class);
        List<Long> typeIdList = resourceCenterResourceCrossoverService.getDownwardCiIdListByCiIdList(Arrays.asList(searchVo.getTypeId()));
//        System.out.println("0:" + (System.currentTimeMillis() - startTime));
        searchVo.setTypeIdList(typeIdList);
        List<ResourceInfo> unavailableResourceInfoList = new ArrayList<>();
        List<InspectResourceVo> inspectResourceList = new ArrayList<>();
//        JSONObject paramObj = (JSONObject) JSONObject.toJSON(searchVo);
        List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList = new ArrayList<>();
        JSONObject commonConditionObj = new JSONObject(paramObj);
        commonConditionObj.put("typeIdList", typeIdList);
        commonConditionObj.remove("inspectStatusList");
        biConsumerList.add(resourceCenterCustomGenerateSqlCrossoverService.getBiConsumerByCommonCondition(commonConditionObj, unavailableResourceInfoList));
        biConsumerList.add(resourceCenterCustomGenerateSqlCrossoverService.getBiConsumerByProtocolIdList(searchVo.getProtocolIdList(), unavailableResourceInfoList));
        biConsumerList.add(resourceCenterCustomGenerateSqlCrossoverService.getBiConsumerByTagIdList(searchVo.getTagIdList(), unavailableResourceInfoList));
        biConsumerList.add(resourceCenterCustomGenerateSqlCrossoverService.getBiConsumerByKeyword(searchVo.getKeyword(), unavailableResourceInfoList));
        biConsumerList.add(getBiConsumerByJobIdAndInspectJobPhaseNodeStatusList(searchVo.getJobId(), searchVo.getInspectJobPhaseNodeStatusList()));
        biConsumerList.add(getBiConsumerByInspectStatusList(searchVo.getInspectStatusList()));

        List<ResourceVo> resourceList = resourceCenterCommonGenerateSqlCrossoverService.getResourceList("resource_ipobject", getTheadList(), biConsumerList, searchVo, unavailableResourceInfoList);
        if (CollectionUtils.isEmpty(resourceList)) {
            TableResultUtil.getResult(resourceList, searchVo);
        }
        List<Long> idList = resourceList.stream().map(ResourceVo::getId).collect(Collectors.toList());
        Map<Long, List<AccountVo>> resourceAccountVoMap = resourceCenterResourceCrossoverService.getResourceAccountByResourceIdList(idList);
        Map<Long, List<TagVo>> resourceTagVoMap = resourceCenterResourceCrossoverService.getResourceTagByResourceIdList(idList);
        List<InspectResourceScriptVo> resourceScriptVoList = inspectMapper.getResourceScriptListByResourceIdList(idList);
        Map<Long, InspectResourceScriptVo> resourceScriptMap = resourceScriptVoList.stream().collect(Collectors.toMap(e -> e.getResourceId(), e -> e));
        List<CiEntityInspectVo> ciEntityInspectList = inspectMapper.getCiEntityInspectByJobIdAndCiEntityIdList(searchVo.getJobId(), idList);
        Map<Long, CiEntityInspectVo> ciEntityInspectMap = ciEntityInspectList.stream().collect(Collectors.toMap(e -> e.getCiEntityId(), e -> e));
        List<AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeList = autoexecJobMapper.getAutoexecJobNodeListByJobIdAndResourceIdList(searchVo.getJobId(), idList);
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
            CiEntityInspectVo ciEntityInspectVo = ciEntityInspectMap.get(id);
            if (ciEntityInspectVo != null) {
                inspectResourceVo.setInspectStatus(ciEntityInspectVo.getInspectStatus());
                inspectResourceVo.setInspectTime(ciEntityInspectVo.getInspectTime());
            }
            inspectResourceList.add(inspectResourceVo);
        }
//        String sql = getResourceCountSql(searchVo, unavailableResourceInfoList);
////        System.out.println("0.5:" + (System.currentTimeMillis() - startTime));
//        if (StringUtils.isBlank(sql)) {
//            TableResultUtil.getResult(inspectResourceVoList, searchVo);
//        }
////        System.out.println(sql + ";");
//        int count = inspectMapper.getInspectResourceCountNew(sql);
////        System.out.println("1:" + (System.currentTimeMillis() - startTime));
////        int resourceCount = inspectMapper.getInspectAutoexecJobNodeResourceCount(searchVo, jobId, TenantContext.get().getDataDbName());
////        if (!Objects.equals(count, resourceCount)) {
////            System.out.println("count=" + count);
////            System.out.println("resourceCount=" + resourceCount);
////        }
//        if (count == 0) {
//            return TableResultUtil.getResult(inspectResourceVoList, searchVo);
//        }
//        searchVo.setRowNum(count);
//        sql = getResourceIdListSql(searchVo, unavailableResourceInfoList);
//        if (StringUtils.isBlank(sql)) {
//            return TableResultUtil.getResult(inspectResourceVoList, searchVo);
//        }
////        System.out.println(sql + ";");
//        List<Long> idList = inspectMapper.getInspectResourceIdListNew(sql);
////        System.out.println("2:" + (System.currentTimeMillis() - startTime));
////        List<Long> resourceIdList = inspectMapper.getInspectAutoexecJobNodeResourceIdList(searchVo, jobId, TenantContext.get().getDataDbName());
////        if (!Objects.equals(idList.size(), resourceIdList.size())) {
////            System.out.println("idList.size()=" + idList.size());
////            System.out.println("resourceIdList.size()=" + resourceIdList.size());
////        }
////        for (int i = 0; i < idList.size(); i++) {
////            if (!Objects.equals(resourceIdList.get(i), idList.get(i))) {
////                System.out.println("resourceIdList[" + i + "]=" + resourceIdList.get(i));
////                System.out.println("idList[" + i + "]=" + idList.get(i));
////            }
////        }
//        if (CollectionUtils.isEmpty(idList)) {
//            return TableResultUtil.getResult(inspectResourceVoList, searchVo);
//        }
//        sql = getResourceListByIdListSql(idList, unavailableResourceInfoList);
//        if (StringUtils.isBlank(sql)) {
//            return TableResultUtil.getResult(inspectResourceVoList, searchVo);
//        }
////        System.out.println(sql + ";");
//        inspectResourceVoList = inspectMapper.getInspectResourceListByIdListNew(sql);
////        System.out.println("3:" + (System.currentTimeMillis() - startTime));
//        if (CollectionUtils.isNotEmpty(inspectResourceVoList)) {
//            inspectReportService.addInspectResourceOtherInfo(inspectResourceVoList);
////            System.out.println("4:" + (System.currentTimeMillis() - startTime));
//            List<CiEntityInspectVo> ciEntityInspectList = inspectMapper.getCiEntityInspectByJobIdAndCiEntityIdList(searchVo.getJobId(), idList);
////            System.out.println("5:" + (System.currentTimeMillis() - startTime));
//            List<AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeList = autoexecJobMapper.getAutoexecJobNodeListByJobIdAndResourceIdList(searchVo.getJobId(), idList);
////            System.out.println("6:" + (System.currentTimeMillis() - startTime));
//            Map<Long, AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeMap = autoexecJobPhaseNodeList.stream().collect(Collectors.toMap(e -> e.getResourceId(), e -> e));
//            Map<Long, CiEntityInspectVo> ciEntityInspectMap = ciEntityInspectList.stream().collect(Collectors.toMap(e -> e.getCiEntityId(), e -> e));
//            for (InspectResourceVo inspectResourceVo : inspectResourceVoList) {
//                CiEntityInspectVo ciEntityInspectVo = ciEntityInspectMap.get(inspectResourceVo.getId());
//                if (ciEntityInspectVo != null) {
//                    inspectResourceVo.setInspectStatus(ciEntityInspectVo.getInspectStatus());
//                    inspectResourceVo.setInspectTime(ciEntityInspectVo.getInspectTime());
//                }
//                AutoexecJobPhaseNodeVo autoexecJobPhaseNodeVo = autoexecJobPhaseNodeMap.get(inspectResourceVo.getId());
//                if (autoexecJobPhaseNodeVo != null) {
////                    autoexecJobPhaseNodeVo.setResourceId(null);
//                    inspectResourceVo.setJobPhaseNodeVo(autoexecJobPhaseNodeVo);
//                } else {
//                    inspectResourceVo.setJobPhaseNodeVo(null);
//                }
//            }
//        }
////        List<InspectResourceVo> inspectResourceVoList2 = inspectMapper.getInspectResourceListByIdListAndJobId(resourceIdList, jobId, TenantContext.get().getDataDbName());
////        Map<Long, InspectResourceVo> inspectResourceMap = inspectResourceVoList2.stream().collect(Collectors.toMap(InspectResourceVo::getId, e -> e));
////        List<InspectResourceScriptVo> resourceScriptVoList = inspectMapper.getResourceScriptListByResourceIdList(resourceIdList);
////        if (CollectionUtils.isNotEmpty(resourceScriptVoList)) {
////            for (InspectResourceScriptVo resourceScriptVo : resourceScriptVoList) {
////                inspectResourceMap.get(resourceScriptVo.getResourceId()).setScript(resourceScriptVo);
////            }
////        }
////        Map<Long, List<AccountVo>> resourceAccountVoMap = resourceCenterResourceCrossoverService.getResourceAccountByResourceIdList(resourceIdList);
////        for (InspectResourceVo inspectResourceVo : inspectResourceVoList2) {
////            Long id = inspectResourceVo.getId();
////            List<AccountVo> accountVoList = resourceAccountVoMap.get(id);
////            if (CollectionUtils.isNotEmpty(accountVoList)) {
////                inspectResourceVo.setAccountList(accountVoList);
////            }
////        }
////        for (int i = 0; i < inspectResourceVoList.size(); i++) {
////            InspectResourceVo resourceVo3 = inspectResourceVoList2.get(i);
////            InspectResourceVo resourceVo = inspectResourceVoList.get(i);
////            if (!Objects.equals(JSONObject.toJSONString(resourceVo3), JSONObject.toJSONString(resourceVo))) {
////                System.out.println("resourceVo3[" + i + "]=" + JSONObject.toJSONString(resourceVo3));
////                System.out.println("resourceVo [" + i + "]=" + JSONObject.toJSONString(resourceVo));
////            }
////        }
        return TableResultUtil.getResult(inspectResourceList, searchVo);
    }

    private List<ResourceInfo> getTheadList() {
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
//        theadList.add(new ResourceInfo("resource_ipobject", "inspect_status"));
//        theadList.add(new ResourceInfo("resource_ipobject", "inspect_time"));
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

    private BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByJobIdAndInspectJobPhaseNodeStatusList(Long jobId, List<String> inspectJobPhaseNodeStatusList) {
        BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> biConsumer = new BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>() {
            @Override
            public void accept(ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, PlainSelect plainSelect) {
                Table mainTable = (Table) plainSelect.getFromItem();
                Table table = new Table("autoexec_job_phase_node").withAlias(new Alias("f").withUseAs(false));
                EqualsTo equalsTo = new EqualsTo()
                        .withLeftExpression(new Column(table, "resource_id"))
                        .withRightExpression(new Column(mainTable, "id"));
                EqualsTo equalsTo2 = new EqualsTo()
                        .withLeftExpression(new Column(table, "job_id"))
                        .withRightExpression(new LongValue(jobId));
                AndExpression andExpression = new AndExpression(equalsTo, equalsTo2);
                if (CollectionUtils.isNotEmpty(inspectJobPhaseNodeStatusList)) {
                    InExpression inExpression = new InExpression();
                    inExpression.setLeftExpression(new Column(table, "status"));
                    ExpressionList expressionList = new ExpressionList();
                    for (String status : inspectJobPhaseNodeStatusList) {
                        expressionList.addExpressions(new StringValue(status));
                    }
                    inExpression.setRightItemsList(expressionList);
                    andExpression = new AndExpression(andExpression, inExpression);
                }
                Join join2 = new Join().withRightItem(table).addOnExpression(andExpression);
                plainSelect.addJoins(join2);
            }
        };
        return biConsumer;
    }

    private BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByInspectStatusList(List<String> inspectStatusList) {
        BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> biConsumer = new BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>() {
            @Override
            public void accept(ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, PlainSelect plainSelect) {
                if (CollectionUtils.isNotEmpty(inspectStatusList)) {
                    Table mainTable = (Table) plainSelect.getFromItem();
                    Table table = new Table("cmdb_cientity_inspect").withAlias(new Alias("g").withUseAs(false));
                    EqualsTo equalsTo = new EqualsTo()
                            .withLeftExpression(new Column(table, "ci_entity_id"))
                            .withRightExpression(new Column(mainTable, "id"));
                    EqualsTo equalsTo2 = new EqualsTo()
                            .withLeftExpression(new Column(table, "job_id"))
                            .withRightExpression(new Column(new Table("f"), "job_id"));
                    InExpression inExpression = new InExpression();
                    inExpression.setLeftExpression(new Column(table, "inspect_status"));
                    ExpressionList expressionList = new ExpressionList();
                    for (String inspectStatus : inspectStatusList) {
                        expressionList.addExpressions(new StringValue(inspectStatus));
                    }
                    inExpression.setRightItemsList(expressionList);
                    Join join = new Join().withRightItem(table).addOnExpression(new AndExpression(new AndExpression(equalsTo, equalsTo2), inExpression));
                    plainSelect.addJoins(join);
                }
            }
        };
        return biConsumer;
    }
}

/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.cmdb.crossover.IResourceCenterCommonGenerateSqlCrossoverService;
import codedriver.framework.cmdb.crossover.IResourceCenterCustomGenerateSqlCrossoverService;
import codedriver.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceInfo;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.cmdb.utils.ResourceSearchGenerateSqlUtil;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dao.mapper.InspectMapper;
import codedriver.framework.inspect.dto.InspectResourceScriptVo;
import codedriver.framework.inspect.dto.InspectResourceVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.inspect.service.InspectReportService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class InspectResourceReportSearchApi extends PrivateApiComponentBase {

    @Resource
    InspectMapper inspectMapper;

    @Resource
    InspectReportService inspectReportService;

    @Override
    public String getName() {
        return "获取巡检资产报告列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "模糊搜索"),
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "id列表，用于刷新状态时精确匹配数据"),
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
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = ResourceVo[].class, desc = "数据列表")
    })
    @Description(desc = "获取巡检资产报告接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
//        ResourceSearchVo searchVo = JSON.toJavaObject(paramObj, ResourceSearchVo.class);
//        Long typeId = searchVo.getTypeId();
//        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
//        CiVo ciVo = ciCrossoverMapper.getCiById(typeId);
//        if (ciVo == null) {
//            throw new CiNotFoundException(typeId);
//        }
//        searchVo.setLft(ciVo.getLft());
//        searchVo.setRht(ciVo.getRht());
//        return TableResultUtil.getResult(inspectReportService.getInspectResourceReportList(searchVo), searchVo);
        List<InspectResourceVo> inspectResourceVoList = new ArrayList<>();
        List<ResourceInfo> unavailableResourceInfoList = new ArrayList<>();
        IResourceCenterResourceCrossoverService resourceCenterResourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
        IResourceCenterCommonGenerateSqlCrossoverService resourceCenterCommonGenerateSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterCommonGenerateSqlCrossoverService.class);
        IResourceCenterCustomGenerateSqlCrossoverService resourceCenterCustomGenerateSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterCustomGenerateSqlCrossoverService.class);
        ResourceSearchVo searchVo = JSONObject.toJavaObject(paramObj, ResourceSearchVo.class);
        JSONArray idArray = paramObj.getJSONArray("idList");
        if (CollectionUtils.isNotEmpty(idArray)) {
            List<Long> idList = idArray.toJavaList(Long.class);
            String sql = resourceCenterCommonGenerateSqlCrossoverService.getResourceListByIdListSql(inspectReportService.getTheadList(), idList, unavailableResourceInfoList);
            if (StringUtils.isBlank(sql)) {
                return TableResultUtil.getResult(inspectResourceVoList, searchVo);
            }
            List<ResourceVo> resourceList = resourceCenterCommonGenerateSqlCrossoverService.getResourceList(sql);
            if (CollectionUtils.isEmpty(resourceList)) {
                return TableResultUtil.getResult(inspectResourceVoList, searchVo);
            }
            inspectResourceVoList = inspectReportService.convertInspectResourceList(resourceList);
            return TableResultUtil.getResult(inspectResourceVoList, searchVo);
        }
        List<Long> typeIdList = resourceCenterResourceCrossoverService.getDownwardCiIdListByCiIdList(Arrays.asList(searchVo.getTypeId()));
        List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList = new ArrayList<>();
        JSONObject commonConditionObj = new JSONObject(paramObj);
        commonConditionObj.put("typeIdList", typeIdList);
        biConsumerList.add(resourceCenterCommonGenerateSqlCrossoverService.getBiConsumerByCommonCondition(commonConditionObj, unavailableResourceInfoList));
        biConsumerList.add(resourceCenterCustomGenerateSqlCrossoverService.getBiConsumerByProtocolIdList(paramObj, unavailableResourceInfoList));
        biConsumerList.add(resourceCenterCustomGenerateSqlCrossoverService.getBiConsumerByTagIdList(paramObj, unavailableResourceInfoList));
        biConsumerList.add(resourceCenterCustomGenerateSqlCrossoverService.getBiConsumerByKeyword(paramObj, unavailableResourceInfoList));
        biConsumerList.add(inspectReportService.getBiConsumerByInspectJobPhaseNodeStatusList(searchVo.getInspectJobPhaseNodeStatusList()));

        List<ResourceVo> resourceList = resourceCenterCommonGenerateSqlCrossoverService.getResourceList(biConsumerList, searchVo, unavailableResourceInfoList, "resource_ipobject", inspectReportService.getTheadList());
        if (CollectionUtils.isEmpty(resourceList)) {
            TableResultUtil.getResult(resourceList, searchVo);
        }
        inspectResourceVoList = inspectReportService.convertInspectResourceList(resourceList);
////        Long typeId = paramObj.getLong("typeId");
////        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
////        CiVo ciVo = ciCrossoverMapper.getCiById(typeId);
////        if (ciVo == null) {
////            throw new CiNotFoundException(typeId);
////        }
////        searchVo.setLft(ciVo.getLft());
////        searchVo.setRht(ciVo.getRht());
//
//        String sql = inspectReportService.getResourceCountSql(searchVo, unavailableResourceInfoList);
//        if (StringUtils.isBlank(sql)) {
//            return TableResultUtil.getResult(inspectResourceVoList, searchVo);
//        }
////        System.out.println(sql + ";");
////        long startTime = System.currentTimeMillis();
//        int count = inspectMapper.getInspectResourceCountNew(sql);
////        System.out.println((System.currentTimeMillis() - startTime));
////        int resourceCount = inspectMapper.getInspectResourceCount(searchVo);
////        if (!Objects.equals(count, resourceCount)) {
////            System.out.println("count=" + count);
////            System.out.println("resourceCount=" + resourceCount);
////        }
//        if (count > 0) {
//            searchVo.setRowNum(count);
//            sql = inspectReportService.getResourceIdListSql(searchVo, unavailableResourceInfoList);
//            if (StringUtils.isBlank(sql)) {
//                return TableResultUtil.getResult(inspectResourceVoList, searchVo);
//            }
////            System.out.println(sql + ";");
//            List<Long> idList = inspectMapper.getInspectResourceIdListNew(sql);
////            System.out.println((System.currentTimeMillis() - startTime));
////            List<Long> resourceIdList = inspectMapper.getInspectResourceIdList(searchVo);
////            if (!Objects.equals(idList.size(), resourceIdList.size())) {
////                System.out.println("idList.size()=" + idList.size());
////                System.out.println("resourceIdList.size()=" + resourceIdList.size());
////            }
////            for (int i = 0; i < idList.size(); i++) {
////                if (!Objects.equals(resourceIdList.get(i), idList.get(i))) {
////                    System.out.println("resourceIdList[" + i + "]=" + resourceIdList.get(i));
////                    System.out.println("idList[" + i + "]=" + idList.get(i));
////                }
////            }
//            if (CollectionUtils.isNotEmpty(idList)) {
//                sql = inspectReportService.getResourceListByIdListSql(idList, unavailableResourceInfoList);
//                if (StringUtils.isBlank(sql)) {
//                    return TableResultUtil.getResult(inspectResourceVoList, searchVo);
//                }
//                inspectResourceVoList = inspectMapper.getInspectResourceListByIdListNew(sql);
//                if (CollectionUtils.isNotEmpty(inspectResourceVoList)) {
//                    inspectReportService.addInspectResourceOtherInfo(inspectResourceVoList);
//                }
////                List<InspectResourceVo> inspectResourceVoList2 = inspectMapper.getInspectResourceListByIdList(resourceIdList, TenantContext.get().getDataDbName());
////                Map<Long, InspectResourceVo> inspectResourceMap = inspectResourceVoList2.stream().collect(Collectors.toMap(InspectResourceVo::getId, e -> e));
////                List<InspectResourceScriptVo> resourceScriptVoList = inspectMapper.getResourceScriptListByResourceIdList(resourceIdList);
////                if (CollectionUtils.isNotEmpty(resourceScriptVoList)) {
////                    for (InspectResourceScriptVo resourceScriptVo : resourceScriptVoList) {
////                        inspectResourceMap.get(resourceScriptVo.getResourceId()).setScript(resourceScriptVo);
////                    }
////                }
////                Map<Long, List<AccountVo>> resourceAccountVoMap = resourceCenterResourceCrossoverService.getResourceAccountByResourceIdList(resourceIdList);
////                for (InspectResourceVo inspectResourceVo : inspectResourceVoList2) {
////                    Long id = inspectResourceVo.getId();
////                    List<AccountVo> accountVoList = resourceAccountVoMap.get(id);
////                    if (CollectionUtils.isNotEmpty(accountVoList)) {
////                        inspectResourceVo.setAccountList(accountVoList);
////                    }
////                }
////                for (int i = 0; i < inspectResourceVoList.size(); i++) {
////                    ResourceVo resourceVo3 = inspectResourceVoList2.get(i);
////                    ResourceVo resourceVo = inspectResourceVoList.get(i);
////                    if (!Objects.equals(JSONObject.toJSONString(resourceVo3), JSONObject.toJSONString(resourceVo))) {
////                        System.out.println("resourceVo3[" + i + "]=" + JSONObject.toJSONString(resourceVo3));
////                        System.out.println("resourceVo [" + i + "]=" + JSONObject.toJSONString(resourceVo));
////                    }
////                }
//            }
//        }
        return TableResultUtil.getResult(inspectResourceVoList, searchVo);
    }

//    private List<InspectResourceVo> convertInspectResourceList(List<ResourceVo> resourceList) {
//        List<InspectResourceVo> inspectResourceVoList = new ArrayList<>();
//        List<Long> idList = resourceList.stream().map(ResourceVo::getId).collect(Collectors.toList());
//        IResourceCenterResourceCrossoverService resourceCenterResourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
//        Map<Long, List<AccountVo>> resourceAccountVoMap = resourceCenterResourceCrossoverService.getResourceAccountByResourceIdList(idList);
//        Map<Long, List<TagVo>> resourceTagVoMap = resourceCenterResourceCrossoverService.getResourceTagByResourceIdList(idList);
//        List<InspectResourceScriptVo> resourceScriptVoList = inspectMapper.getResourceScriptListByResourceIdList(idList);
//        Map<Long, InspectResourceScriptVo> resourceScriptMap = resourceScriptVoList.stream().collect(Collectors.toMap(e -> e.getResourceId(), e -> e));
//        List<AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeList = autoexecJobMapper.getAutoexecJobNodeListByResourceIdList(idList);
//        Map<Long, AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeMap = autoexecJobPhaseNodeList.stream().collect(Collectors.toMap(e -> e.getResourceId(), e -> e));
//        for (ResourceVo resourceVo : resourceList) {
//            InspectResourceVo inspectResourceVo = new InspectResourceVo(resourceVo);
//            Long id = inspectResourceVo.getId();
//            List<AccountVo> accountVoList = resourceAccountVoMap.get(id);
//            if (CollectionUtils.isNotEmpty(accountVoList)) {
//                inspectResourceVo.setAccountList(accountVoList);
//            }
//            List<TagVo> tagVoList = resourceTagVoMap.get(id);
//            if (CollectionUtils.isNotEmpty(tagVoList)) {
//                inspectResourceVo.setTagList(tagVoList.stream().map(TagVo::getName).collect(Collectors.toList()));
//            }
//            InspectResourceScriptVo inspectResourceScriptVo = resourceScriptMap.get(id);
//            if (inspectResourceScriptVo != null) {
//                inspectResourceVo.setScript(inspectResourceScriptVo);
//            }
//            AutoexecJobPhaseNodeVo autoexecJobPhaseNodeVo = autoexecJobPhaseNodeMap.get(id);
//            if (autoexecJobPhaseNodeVo != null) {
//                inspectResourceVo.setJobPhaseNodeVo(autoexecJobPhaseNodeVo);
//            }
//            inspectResourceVoList.add(inspectResourceVo);
//        }
//        return inspectResourceVoList;
//    }
//
//    private BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByInspectJobPhaseNodeStatusList(List<String> inspectJobPhaseNodeStatusList) {
//        BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> biConsumer = new BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>() {
//            @Override
//            public void accept(ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, PlainSelect plainSelect) {
//                if (CollectionUtils.isNotEmpty(inspectJobPhaseNodeStatusList)) {
//                    Table mainTable = (Table) plainSelect.getFromItem();
//                    Table table = new Table("autoexec_job_resource_inspect").withAlias(new Alias("e").withUseAs(false));
//                    EqualsTo equalsTo = new EqualsTo()
//                            .withLeftExpression(new Column(table, "resource_id"))
//                            .withRightExpression(new Column(mainTable, "id"));
//                    Join join = new Join().withRightItem(table).addOnExpression(equalsTo);
//                    plainSelect.addJoins(join);
//                    Table table2 = new Table("autoexec_job_phase_node").withAlias(new Alias("f").withUseAs(false));
//                    EqualsTo equalsTo2 = new EqualsTo()
//                            .withLeftExpression(new Column(table2, "resource_id"))
//                            .withRightExpression(new Column(mainTable, "id"));
//                    EqualsTo equalsTo3 = new EqualsTo()
//                            .withLeftExpression(new Column(table2, "job_phase_id"))
//                            .withRightExpression(new Column(table, "phase_id"));
//                    AndExpression andExpression = new AndExpression(equalsTo2, equalsTo3);
//                    InExpression inExpression = new InExpression();
//                    inExpression.setLeftExpression(new Column(table2, "status"));
//                    ExpressionList expressionList = new ExpressionList();
//                    for (String status : inspectJobPhaseNodeStatusList) {
//                        expressionList.addExpressions(new StringValue(status));
//                    }
//                    inExpression.setRightItemsList(expressionList);
//                    Join join2 = new Join().withRightItem(table2).addOnExpression(new AndExpression(andExpression, inExpression));
//                    plainSelect.addJoins(join2);
//                }
//            }
//        };
//        return biConsumer;
//    }

    @Override
    public String getToken() {
        return "inspect/resource/report/search";
    }

//    private List<ResourceInfo> getTheadList() {
//        List<ResourceInfo> theadList = new ArrayList<>();
//        theadList.add(new ResourceInfo("resource_ipobject", "id"));
//        //1.IP地址:端口
//        theadList.add(new ResourceInfo("resource_ipobject", "ip"));
//        theadList.add(new ResourceInfo("resource_softwareservice", "port"));
//        //2.类型
//        theadList.add(new ResourceInfo("resource_ipobject", "type_id"));
//        theadList.add(new ResourceInfo("resource_ipobject", "type_name"));
//        theadList.add(new ResourceInfo("resource_ipobject", "type_label"));
//        //3.名称
//        theadList.add(new ResourceInfo("resource_ipobject", "name"));
//        //4.监控状态
//        theadList.add(new ResourceInfo("resource_ipobject", "monitor_status"));
//        theadList.add(new ResourceInfo("resource_ipobject", "monitor_time"));
//        //5.巡检状态
//        theadList.add(new ResourceInfo("resource_ipobject", "inspect_status"));
//        theadList.add(new ResourceInfo("resource_ipobject", "inspect_time"));
//        //12.网络区域
//        theadList.add(new ResourceInfo("resource_ipobject", "network_area"));
//        //14.维护窗口
//        theadList.add(new ResourceInfo("resource_ipobject", "maintenance_window"));
//        //16.描述
//        theadList.add(new ResourceInfo("resource_ipobject", "description"));
//        //6.模块
////        theadList.add(new ResourceInfo("resource_ipobject_appmodule", "app_module_id"));
////        theadList.add(new ResourceInfo("resource_ipobject_appmodule", "app_module_name"));
////        theadList.add(new ResourceInfo("resource_ipobject_appmodule", "app_module_abbr_name"));
//        //7.应用
////        theadList.add(new ResourceInfo("resource_appmodule_appsystem", "app_system_id"));
////        theadList.add(new ResourceInfo("resource_appmodule_appsystem", "app_system_name"));
////        theadList.add(new ResourceInfo("resource_appmodule_appsystem", "app_system_abbr_name"));
//        //8.IP列表
//        theadList.add(new ResourceInfo("resource_ipobject_allip", "allip_id"));
//        theadList.add(new ResourceInfo("resource_ipobject_allip", "allip_ip"));
//        theadList.add(new ResourceInfo("resource_ipobject_allip", "allip_label"));
//        //9.所属部门
//        theadList.add(new ResourceInfo("resource_ipobject_bg", "bg_id"));
//        theadList.add(new ResourceInfo("resource_ipobject_bg", "bg_name"));
//        //10.所有者
//        theadList.add(new ResourceInfo("resource_ipobject_owner", "user_id"));
//        theadList.add(new ResourceInfo("resource_ipobject_owner", "user_uuid"));
//        theadList.add(new ResourceInfo("resource_ipobject_owner", "user_name"));
//        //11.资产状态
//        theadList.add(new ResourceInfo("resource_ipobject_state", "state_id"));
//        theadList.add(new ResourceInfo("resource_ipobject_state", "state_name"));
//        theadList.add(new ResourceInfo("resource_ipobject_state", "state_label"));
//        //环境状态
////        theadList.add(new ResourceInfo("resource_softwareservice_env", "env_id"));
////        theadList.add(new ResourceInfo("resource_softwareservice_env", "env_name"));
//        return theadList;
//    }
}

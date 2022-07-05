/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceInfo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dao.mapper.InspectMapper;
import codedriver.framework.inspect.dto.InspectResourceVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.inspect.service.InspectReportService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class InspectResourceReportSearchApi extends PrivateApiComponentBase {

    @Resource
    private InspectReportService inspectReportService;

    @Resource
    InspectMapper inspectMapper;

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
        IResourceCenterResourceCrossoverService resourceCenterResourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
        ResourceSearchVo searchVo;
        JSONArray idArray = paramObj.getJSONArray("idList");
        if (CollectionUtils.isNotEmpty(idArray)) {
            searchVo = new ResourceSearchVo();
            searchVo.setDefaultValue(idArray);
        } else {
            searchVo = resourceCenterResourceCrossoverService.assembleResourceSearchVo(paramObj);
        }
//        Long typeId = paramObj.getLong("typeId");
//        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
//        CiVo ciVo = ciCrossoverMapper.getCiById(typeId);
//        if (ciVo == null) {
//            throw new CiNotFoundException(typeId);
//        }
//        searchVo.setLft(ciVo.getLft());
//        searchVo.setRht(ciVo.getRht());

        List<ResourceInfo> unavailableResourceInfoList = new ArrayList<>();
        String sql = inspectReportService.getResourceCountSql(searchVo, unavailableResourceInfoList);
        if (StringUtils.isBlank(sql)) {
            TableResultUtil.getResult(inspectResourceVoList, searchVo);
        }
//        System.out.println(sql + ";");
//        long startTime = System.currentTimeMillis();
        int count = inspectMapper.getInspectResourceCountNew(sql);
//        System.out.println((System.currentTimeMillis() - startTime));
//        int resourceCount = inspectMapper.getInspectResourceCount(searchVo);
//        if (!Objects.equals(count, resourceCount)) {
//            System.out.println("count=" + count);
//            System.out.println("resourceCount=" + resourceCount);
//        }
        if (count > 0) {
            searchVo.setRowNum(count);
            sql = inspectReportService.getResourceIdListSql(searchVo, unavailableResourceInfoList);
            if (StringUtils.isBlank(sql)) {
                TableResultUtil.getResult(inspectResourceVoList, searchVo);
            }
//            System.out.println(sql + ";");
            List<Long> idList = inspectMapper.getInspectResourceIdListNew(sql);
//            System.out.println((System.currentTimeMillis() - startTime));
//            List<Long> resourceIdList = inspectMapper.getInspectResourceIdList(searchVo);
//            if (!Objects.equals(idList.size(), resourceIdList.size())) {
//                System.out.println("idList.size()=" + idList.size());
//                System.out.println("resourceIdList.size()=" + resourceIdList.size());
//            }
//            for (int i = 0; i < idList.size(); i++) {
//                if (!Objects.equals(resourceIdList.get(i), idList.get(i))) {
//                    System.out.println("resourceIdList[" + i + "]=" + resourceIdList.get(i));
//                    System.out.println("idList[" + i + "]=" + idList.get(i));
//                }
//            }
            if (CollectionUtils.isNotEmpty(idList)) {
                sql = inspectReportService.getResourceListByIdListSql(idList, unavailableResourceInfoList);
                if (StringUtils.isBlank(sql)) {
                    TableResultUtil.getResult(inspectResourceVoList, searchVo);
                }
                inspectResourceVoList = inspectMapper.getInspectResourceListByIdListNew(sql);
                if (CollectionUtils.isNotEmpty(inspectResourceVoList)) {
                    inspectReportService.addInspectResourceOtherInfo(inspectResourceVoList);
                }
//                List<InspectResourceVo> inspectResourceVoList2 = inspectMapper.getInspectResourceListByIdList(resourceIdList, TenantContext.get().getDataDbName());
//                Map<Long, InspectResourceVo> inspectResourceMap = inspectResourceVoList2.stream().collect(Collectors.toMap(InspectResourceVo::getId, e -> e));
//                List<InspectResourceScriptVo> resourceScriptVoList = inspectMapper.getResourceScriptListByResourceIdList(resourceIdList);
//                if (CollectionUtils.isNotEmpty(resourceScriptVoList)) {
//                    for (InspectResourceScriptVo resourceScriptVo : resourceScriptVoList) {
//                        inspectResourceMap.get(resourceScriptVo.getResourceId()).setScript(resourceScriptVo);
//                    }
//                }
//                Map<Long, List<AccountVo>> resourceAccountVoMap = resourceCenterResourceCrossoverService.getResourceAccountByResourceIdList(resourceIdList);
//                for (InspectResourceVo inspectResourceVo : inspectResourceVoList2) {
//                    Long id = inspectResourceVo.getId();
//                    List<AccountVo> accountVoList = resourceAccountVoMap.get(id);
//                    if (CollectionUtils.isNotEmpty(accountVoList)) {
//                        inspectResourceVo.setAccountList(accountVoList);
//                    }
//                }
//                for (int i = 0; i < inspectResourceVoList.size(); i++) {
//                    ResourceVo resourceVo3 = inspectResourceVoList2.get(i);
//                    ResourceVo resourceVo = inspectResourceVoList.get(i);
//                    if (!Objects.equals(JSONObject.toJSONString(resourceVo3), JSONObject.toJSONString(resourceVo))) {
//                        System.out.println("resourceVo3[" + i + "]=" + JSONObject.toJSONString(resourceVo3));
//                        System.out.println("resourceVo [" + i + "]=" + JSONObject.toJSONString(resourceVo));
//                    }
//                }
            }
        }
        return TableResultUtil.getResult(inspectResourceVoList, searchVo);
    }

    @Override
    public String getToken() {
        return "inspect/resource/report/search";
    }
}

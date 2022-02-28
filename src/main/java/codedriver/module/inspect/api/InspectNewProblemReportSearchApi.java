package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.InspectStatus;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dto.InspectResourceVo;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.inspect.service.InspectReportService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * @author longrf
 * @date 2022/2/23 3:45 下午
 */
@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class InspectNewProblemReportSearchApi extends PrivateApiComponentBase {

    @Resource
    InspectReportService inspectReportService;

    @Resource
    MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "巡检最新问题报告查询接口";
    }

    @Override
    public String getToken() {
            return "inspect/new/problem/report/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "模糊搜索"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id"),
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
            @Param(name = "tbodyList", explode = InspectResourceVo[].class, desc = "巡检最新问题报告列表")
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray inspectStatusArray = paramObj.getJSONArray("inspectStatusList");
        if (CollectionUtils.isEmpty(inspectStatusArray)) {
            paramObj.put("inspectStatusList", Arrays.asList(InspectStatus.WARN.getValue(), InspectStatus.CRITICAL.getValue(), InspectStatus.FATAL.getValue()));
        }
        IResourceCenterResourceCrossoverService resourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
        ResourceSearchVo searchVo = resourceCrossoverService.assembleResourceSearchVo(paramObj);
        List<InspectResourceVo> inspectResourceVoList= inspectReportService.getInspectResourceReportList(searchVo);

        //补充巡检问题信息
        if (CollectionUtils.isNotEmpty(inspectResourceVoList)) {
            MongoCollection<Document> collection = mongoTemplate.getCollection("INSPECT_REPORTS");
            Document doc = new Document();
            for (InspectResourceVo inspectResourceVo : inspectResourceVoList) {
                doc.put("RESOURCE_ID", inspectResourceVo.getId());
                FindIterable<Document> findIterable = collection.find(doc);
                Document reportDoc = findIterable.first();
                if (MapUtils.isNotEmpty(reportDoc)) {
                    JSONObject reportJson = JSONObject.parseObject(reportDoc.toJson());
                    inspectResourceVo.setInspectResult(reportJson.getJSONObject("_inspect_result"));
                }
            }
        }
        return TableResultUtil.getResult(inspectResourceVoList, searchVo);
    }

}

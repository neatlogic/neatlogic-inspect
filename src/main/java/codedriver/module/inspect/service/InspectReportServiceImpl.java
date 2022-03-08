package codedriver.module.inspect.service;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.dto.sync.CollectionVo;
import codedriver.framework.common.constvalue.InspectStatus;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.dao.mapper.InspectMapper;
import codedriver.framework.inspect.dto.InspectResourceVo;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class InspectReportServiceImpl implements InspectReportService {

    @Resource
    MongoTemplate mongoTemplate;

    @Resource
    InspectMapper inspectMapper;

    @Override
    public Document getInspectReport(Long resourceId, String id, Long jobId) {
        MongoCollection<Document> collection;
        Document doc = new Document();
        if (StringUtils.isNotBlank(id) || (jobId != null && resourceId != null)) {
            //场景1：用id查历史报告
            //场景2：用jobId和resourceId查历史报告
            collection = mongoTemplate.getDb().getCollection("INSPECT_REPORTS_HIS");
        }else {
            //场景3：用resourceId查最新报告
            collection = mongoTemplate.getDb().getCollection("INSPECT_REPORTS");
        }
        if (resourceId != null) {
            doc.put("RESOURCE_ID", resourceId);
        }
        if (StringUtils.isNotBlank(id)) {
            doc.put("_id", new ObjectId(id));
        }
        if (jobId != null) {
            doc.put("_jobid", jobId.toString());
        }

        FindIterable<Document> findIterable = collection.find(doc);
        Document reportDoc = findIterable.first();
        // TODO 通过 Query 的方式 搜不到结果集
        //JSONObject inspectReport = mongoTemplate.findOne(new Query(Criteria.where("MGMT_IP").is("192.168.0.33")), JSONObject.class, "INSPECT_REPORTS");
        //补充
        if (MapUtils.isNotEmpty(reportDoc)) {
            JSONObject reportJson = JSONObject.parseObject(reportDoc.toJson());
            JSONObject inspectResult = reportJson.getJSONObject("_inspect_result");
            if (MapUtils.isNotEmpty(inspectResult)) {
                String name = inspectResult.getString("name");
                CollectionVo collectionVo = mongoTemplate.findOne(new Query(Criteria.where("name").is(name)), CollectionVo.class, "_dictionary");
                if (collectionVo != null) {
                    reportDoc.put("fields", collectionVo.getFields());
                }
            }
            //补充inspectStatus
            reportDoc.put("inspectStatus", InspectStatus.getAllInspectStatusMap());
        }
        return reportDoc;
    }

    @Override
    public List<InspectResourceVo> getInspectAutoexecJobNodeList(Long jobId, ResourceSearchVo searchVo) {
        List<InspectResourceVo> inspectResourceVoList = null;
        if (searchVo.getIdList() == null || CollectionUtils.isNotEmpty(searchVo.getIdList())) {
            int rowNum = inspectMapper.getInspectAutoexecJobNodeResourceCount(searchVo, jobId, TenantContext.get().getDataDbName());
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<ResourceVo> resourceVoList = new ArrayList<>();
                List<Long> resourceIdList = inspectMapper.getInspectAutoexecJobNodeResourceIdList(searchVo, jobId, TenantContext.get().getDataDbName());
                inspectResourceVoList = inspectMapper.getInspectResourceVoListByIdListAndJobId(resourceIdList, jobId, TenantContext.get().getDataDbName());
                if (CollectionUtils.isNotEmpty(inspectResourceVoList)) {
                    resourceVoList.addAll(inspectResourceVoList);
                    IResourceCenterResourceCrossoverService resourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
                    resourceCrossoverService.addResourceTag(resourceIdList, resourceVoList);
                }
            }
        }
        if (inspectResourceVoList == null) {
            inspectResourceVoList = new ArrayList<>();
        }
        return inspectResourceVoList;
    }

    @Override
    public List<InspectResourceVo> getInspectResourceReportList(ResourceSearchVo searchVo) {
        List<InspectResourceVo> inspectResourceVoList = null;
        if (searchVo.getIdList() == null || CollectionUtils.isNotEmpty(searchVo.getIdList())) {
            int rowNum = inspectMapper.getInspectResourceCount(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<ResourceVo> resourceVoList = new ArrayList<>();
                List<Long> resourceIdList = inspectMapper.getInspectResourceIdList(searchVo);
                inspectResourceVoList = inspectMapper.getInspectResourceVoListByIdList(resourceIdList, TenantContext.get().getDataDbName());
                if (CollectionUtils.isNotEmpty(inspectResourceVoList)) {
                    IResourceCenterResourceCrossoverService resourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
                    resourceVoList.addAll(inspectResourceVoList);
                    resourceCrossoverService.addResourceTag(resourceIdList, resourceVoList);
                }
            }
        }
        if (inspectResourceVoList == null) {
            inspectResourceVoList = new ArrayList<>();
        }
        return inspectResourceVoList;
    }
}

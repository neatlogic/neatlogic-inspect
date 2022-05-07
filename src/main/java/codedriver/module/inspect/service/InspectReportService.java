package codedriver.module.inspect.service;

import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.inspect.dto.InspectResourceVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.List;

public interface InspectReportService {

    /**
     * 获取单个资产的巡检报告
     *  可用id查历史报告
     *  可用jobId和resourceId查历史报告
     *  可用resourceId查最新报告
     * @param resourceId
     * @param id
     * @param jobId
     * @return
     */
    Document getInspectReport(Long resourceId, String id, Long jobId);

    /**
     * 获取当前作业的巡检作业节点
     *
     * @param jobId
     * @param searchVo
     * @return
     */
    List<InspectResourceVo> getInspectAutoexecJobNodeList(Long jobId, ResourceSearchVo searchVo);

    /**
     * 获取最新资产巡检报告
     * @param searchVo
     * @return
     */
    List<InspectResourceVo> getInspectResourceReportList(ResourceSearchVo searchVo);

    /**
     * 根据resourceIdList 获取 对应巡检报告
     * @param resourceIdList 资产id列表
     * @return 对应巡检报告
     */
    JSONArray getInspectDetailByResourceIdList(List<Long> resourceIdList);

    /**
     * 根据resourceId 获取 对应巡检报告
     * @param resourceId 资产id
     * @return 对应巡检报告
     */
    JSONObject getInspectDetailByResourceId(Long resourceId);

    /**
     * 循环根据resourceId 获取 对应巡检报告
     * @param resourceId 资产id
     * @param collection mongoCollection
     * @return 对应巡检报告
     */
    JSONObject getBatchInspectDetailByResourceId(Long resourceId, MongoCollection<Document> collection);
}

package codedriver.module.inspect.service;

import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceInfo;
import codedriver.framework.cmdb.utils.ResourceSearchGenerateSqlUtil;
import codedriver.framework.inspect.dto.InspectResourceVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.MongoCollection;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.apache.poi.ss.usermodel.Workbook;
import org.bson.Document;

import java.util.List;
import java.util.Map;

public interface InspectReportService {

    /**
     * 获取单个资产的巡检报告
     * 可用id查历史报告
     * 可用jobId和resourceId查历史报告
     * 可用resourceId查最新报告
     *
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
     *
     * @param searchVo
     * @return
     */
//    List<InspectResourceVo> getInspectResourceReportList(ResourceSearchVo searchVo);

    /**
     * 根据resourceIdList 获取 对应巡检报告
     *
     * @param resourceIdList 资产id列表
     * @return 对应巡检报告
     */
    JSONObject getInspectDetailByResourceIdList(List<Long> resourceIdList);

    /**
     * 根据resourceId 获取 对应巡检报告
     *
     * @param resourceId 资产id
     * @return 对应巡检报告
     */
    JSONObject getInspectDetailByResourceId(Long resourceId);

    /**
     * 循环根据resourceId 获取 对应巡检报告
     *
     * @param resourceId 资产id
     * @param collection mongoCollection
     * @return 对应巡检报告
     */
    JSONObject getBatchInspectDetailByResourceId(Long resourceId, MongoCollection<Document> collection);

    /**
     * 从mongodb分析出告警对象，并翻译
     *
     * @param reportJson       资产巡检报告
     * @param alert            告警
     * @param threholdJson     threholdJson
     * @param fieldPathTextMap 用于翻译field
     * @return 告警对象
     */
    Object getInspectAlertObject(JSONObject reportJson, JSONObject alert, JSONObject threholdJson, Map<String, String> fieldPathTextMap);

    /**
     * 递归获取fieldTextMap
     *
     * @param fieldPathTextMap field 和 fieldText map
     * @param parentPath       父节点路径
     * @param subsetArray      子节点数组
     */
    void getFieldPathTextMap(Map<String, String> fieldPathTextMap, String parentPath, JSONArray subsetArray);

    /**
     * 获取巡检最新报告列表Workbook
     *
     * @param searchVo          查询条件
     * @param isNeedAlertDetail 1:会导出具体告警信息；0：不会导出具体告警信息
     * @return
     */
    Workbook getInspectNewProblemReportWorkbook(ResourceSearchVo searchVo, Integer isNeedAlertDetail);

    /**
     * 拼装查询当前页id列表sql语句
     * @param searchVo
     * @param unavailableResourceInfoList
     * @return
     */
    String getResourceIdListSql(ResourceSearchVo searchVo, List<ResourceInfo> unavailableResourceInfoList);

    /**
     * 拼装查询总数sql语句
     * @param searchVo
     * @param unavailableResourceInfoList
     * @return
     */
    String getResourceCountSql(ResourceSearchVo searchVo, List<ResourceInfo> unavailableResourceInfoList);

    /**
     * 根据需要查询的列，生成对应的sql语句
     * @param idList
     * @param unavailableResourceInfoList
     * @return
     */
    String getResourceListByIdListSql(List<Long> idList, List<ResourceInfo> unavailableResourceInfoList);

    /**
     * 添加其他信息，账号、标签、巡检作业状态、脚本
     * @param inspectResourceList
     */
    void addInspectResourceOtherInfo(List<InspectResourceVo> inspectResourceList);
}

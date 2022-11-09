package codedriver.module.inspect.service;

import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.inspect.dto.InspectResourceVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.MongoCollection;
import org.apache.poi.ss.usermodel.Workbook;
import org.bson.Document;

import java.util.Date;
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
    List<InspectResourceVo> getInspectResourceReportList(ResourceSearchVo searchVo);

    /**
     * 根据resourceIdList 获取 对应巡检报告
     *
     * @param resourceIdList 资产id列表
     * @return 对应巡检报告
     */
    JSONObject getInspectDetailByResourceIdList(List<Long> resourceIdList);

    /**
     * 根据resourceIdList 获取 对应日期的巡检报告
     *
     * @param resourceIdList 资产id列表
     * @param startDate      开始日期
     * @param endDate        结束日期
     * @return 对应巡检报告
     */
    JSONObject getInspectDetailByResourceIdListAndDate(List<Long> resourceIdList, Date startDate, Date endDate);

    /**
     * 根据resourceId 获取 对应巡检报告
     *
     * @param resourceIdList 资产id
     * @return 对应巡检报告
     */
    JSONArray getInspectDetailByResourceIdListFromDb(List<Long> resourceIdList);

    /**
     * 循环根据resourceId 获取 对应巡检报告
     *
     * @param resourceIdList 资产id
     * @param collection     mongoCollection
     * @return 对应巡检报告
     */
    JSONArray getInspectDetailByResourceIdListFromDb(List<Long> resourceIdList, MongoCollection<Document> collection);

    /**
     * 循环根据resourceId 获取 对日期的应巡检报告
     *
     * @param resourceId 资产id
     * @param collection mongoCollection
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 对应巡检报告
     */
    JSONArray getInspectDetailByResourceIdListAndDateFromDb(List<Long> resourceId, MongoCollection<Document> collection, Date startDate, Date endDate);


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
     * 根据应用系统ID获取巡检最新报告列表Workbook
     * @param appSystemId 应用系统ID
     * @param isNeedAlertDetail 1:会导出具体告警信息；0：不会导出具体告警信息
     * @return
     */
    Workbook getInspectNewProblemReportWorkbookByAppSystemId(Long appSystemId, Integer isNeedAlertDetail);

    /**
     * 更新巡检告警表数据
     * 获取前一天的数据
     */
    void updateInspectAlertEveryDayData();


    /**
     * 更新巡检告警表数据
     * 如果startDate和endDate都为null，则默认获取前一天的数据
     *
     * @param startDate 开始时间
     * @param endDate   结束时间
     */
    void updateInspectAlertEveryDayData(Date startDate, Date endDate);
}

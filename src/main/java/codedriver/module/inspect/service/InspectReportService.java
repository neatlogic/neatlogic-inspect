package codedriver.module.inspect.service;

import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.inspect.dto.InspectResourceVo;
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
}

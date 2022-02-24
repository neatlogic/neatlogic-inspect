package codedriver.module.inspect.service;

import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.inspect.dto.InspectResourceVo;
import org.bson.Document;

import java.util.List;

public interface InspectReportService {

    Document getInspectReport(Long resourceId, String id, Long jobId);

    /**
     * 获取当前作业的巡检作业节点
     *
     * @param jobId
     * @param searchVo
     * @return
     */
    List<InspectResourceVo> getInspectAutoexecJobNodeList(Long jobId, ResourceSearchVo searchVo);
}

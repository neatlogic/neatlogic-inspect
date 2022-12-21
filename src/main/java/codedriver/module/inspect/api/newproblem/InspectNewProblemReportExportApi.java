package codedriver.module.inspect.api.newproblem;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiCrossoverMapper;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.module.inspect.service.InspectReportService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @author longrf
 * @date 2022/3/1 2:40 下午
 */
@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class InspectNewProblemReportExportApi extends PrivateBinaryStreamApiComponentBase {
    private final static Logger logger = LoggerFactory.getLogger(InspectNewProblemReportExportApi.class);

    @Resource
    InspectReportService inspectReportService;

    @Override
    public String getName() {
        return "导出巡检最新报告列表";
    }

    @Override
    public String getToken() {
        return "inspect/new/problem/report/export";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "模糊搜索"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型ID"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用ID"),
            @Param(name = "protocolIdList", type = ApiParamType.JSONARRAY, desc = "协议id列表"),
            @Param(name = "stateIdList", type = ApiParamType.JSONARRAY, desc = "状态id列表"),
            @Param(name = "vendorIdList", type = ApiParamType.JSONARRAY, desc = "厂商id列表"),
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, desc = "环境id列表"),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "应用系统id列表"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "资源类型id列表"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, desc = "标签id列表"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显的资源ID列表"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "巡检状态列表"),
            @Param(name = "inspectJobPhaseNodeStatusList", type = ApiParamType.JSONARRAY, desc = "巡检作业状态列表"),
            @Param(name = "isNeedAlertDetail", type = ApiParamType.ENUM, rule = "1,0", desc = "1:会导出具体告警信息；0：不会导出具体告警信息;默认为0"),
    })
    @Output({
    })
    @Description(desc = "导出巡检最报告列表接口")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ResourceSearchVo searchVo = JSON.toJavaObject(paramObj, ResourceSearchVo.class);
        Integer isNeedAlertDetail = paramObj.getInteger("isNeedAlertDetail");
        if (isNeedAlertDetail == null) {
            isNeedAlertDetail = 0;
        }
        String fileNameEncode = "";
        Workbook workbook = null;
        Long appSystemId = searchVo.getAppSystemId();
        Long typeId = searchVo.getTypeId();
        if (typeId != null) {
            ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
            CiVo ciVo = ciCrossoverMapper.getCiById(typeId);
            if (ciVo == null) {
                throw new CiNotFoundException(typeId);
            }
            searchVo.setLft(ciVo.getLft());
            searchVo.setRht(ciVo.getRht());
            searchVo.setPageSize(100);
            searchVo.setCurrentPage(1);
            workbook = inspectReportService.getInspectNewProblemReportWorkbook(searchVo, isNeedAlertDetail);
            fileNameEncode = ciVo.getId() + "_" + ciVo.getLabel() + ".xlsx";
        } else if (appSystemId != null) {
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            ResourceVo appSystem = resourceCrossoverMapper.getAppSystemById(appSystemId);
            if (appSystem == null) {
                throw new AppSystemNotFoundException(appSystemId);
            }
            workbook = inspectReportService.getInspectNewProblemReportWorkbookByAppSystemId(appSystemId, isNeedAlertDetail);
            fileNameEncode = appSystemId + "_" +appSystem.getName() + ".xlsx";
        } else {
            throw new ParamNotExistsException("类型ID（typeId）", "应用ID（appSystemId）");
        }
        if (workbook != null) {
            boolean flag = request.getHeader("User-Agent").indexOf("Gecko") > 0;
            if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0 || flag) {
                fileNameEncode = URLEncoder.encode(fileNameEncode, "UTF-8");// IE浏览器
            } else {
                fileNameEncode = new String(fileNameEncode.replace(" ", "").getBytes(StandardCharsets.UTF_8), "ISO8859-1");
            }
            response.setContentType("application/vnd.ms-excel;charset=utf-8");
            response.setHeader("Content-Disposition", " attachment; filename=\"" + fileNameEncode + "\"");
            try (OutputStream os = response.getOutputStream()) {
                workbook.write(os);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

}

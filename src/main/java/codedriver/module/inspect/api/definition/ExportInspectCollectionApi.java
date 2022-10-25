/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.inspect.api.definition;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.InspectStatus;
import codedriver.framework.inspect.auth.INSPECT_MODIFY;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.FileUtil;
import codedriver.framework.util.excel.ExcelBuilder;
import codedriver.framework.util.excel.SheetBuilder;
import codedriver.module.inspect.service.InspectCollectService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author longrf
 * @date 2022/10/24 10:36
 */
@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ExportInspectCollectionApi extends PrivateBinaryStreamApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(ExportInspectCollectionApi.class);

    @Resource
    private InspectCollectService inspectCollectService;

    @Override
    public String getName() {
        return "导出巡检指标和规则";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "inspect/collection/export";
    }

    @Input({
            @Param(name = "name", type = ApiParamType.STRING, desc = "唯一标识"),
            @Param(name = "isAll", type = ApiParamType.INTEGER, isRequired = true, desc = "是否全量（1：全量，0：单个）")
    })
    @Description(desc = "导出巡检指标和规则")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {

        String fileName = "";
        ExcelBuilder builder = new ExcelBuilder(SXSSFWorkbook.class);

        if (paramObj.getInteger("isAll") == 0) {
            JSONObject collectionObj = inspectCollectService.getCollectionByName(paramObj.getString("name"));
            fileName = exportCollection(builder, collectionObj);
        } else {
            fileName = FileUtil.getEncodedFileName("巡检指标及告警规则.xlsx");
            JSONArray allCollection = inspectCollectService.getAllCollection();

            for (int i = 0; i < allCollection.size(); i++) {
                Object object = allCollection.get(i);
                try {
                    JSONObject collectionObj = (JSONObject) object;
                    exportCollection(builder, collectionObj);
                } catch (Exception ex) {
                    logger.error(object.toString() + "导出巡检指标和规则时，第" + (i + 1) + "个字典不是JSONObject，转换失败:" + object.toString() + ex.getMessage(), ex);
                }
            }
        }

        Workbook workbook = builder.build();
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");

        try (OutputStream os = response.getOutputStream()) {
            workbook.write(os);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (workbook != null) {
                ((SXSSFWorkbook) workbook).dispose();
            }
        }
        return null;
    }

    private String exportCollection(ExcelBuilder builder, JSONObject collectionObj) throws UnsupportedEncodingException {
        String fileName = FileUtil.getEncodedFileName(collectionObj.getString("label") + "(" + collectionObj.getString("name") + ")巡检指标及告警规则.xlsx");
        List<String> headerList = new ArrayList<>();
        headerList.add("规则名称");
        headerList.add("级别");
        headerList.add("规则");
        List<String> columnList = new ArrayList<>();
        columnList.add("规则名称");
        columnList.add("级别");
        columnList.add("规则");

        //创建工作簿
        SheetBuilder sheetBuilder = builder.withBorderColor(HSSFColor.HSSFColorPredefined.GREY_40_PERCENT)
                .withHeadFontColor(HSSFColor.HSSFColorPredefined.WHITE)
                .withHeadBgColor(HSSFColor.HSSFColorPredefined.GREEN)
                .withColumnWidth(30)
                .addSheet(collectionObj.getString("label") + "(" + collectionObj.getString("name") + ")")
                .withHeaderList(headerList).withColumnList(columnList);

        //规则
        JSONArray thresholds = collectionObj.getJSONArray("thresholds");
        if (CollectionUtils.isNotEmpty(thresholds)) {

            for (int i = 0; i < thresholds.size(); i++) {
                Object object = thresholds.get(i);
                if (object == null) {
                    continue;
                }
                Map<String, Object> dataMap = new HashMap<>();
                try {
                    JSONObject jsonObject = (JSONObject) object;
                    dataMap.put("规则名称", jsonObject.getString("name"));
                    dataMap.put("级别", InspectStatus.getText(jsonObject.getString("level")));
                    dataMap.put("规则", jsonObject.getString("rule"));
                    sheetBuilder.addData(dataMap);
                } catch (Exception ex) {
                    logger.error("导出巡检指标和规则时，第" + (i + 1) + "个字典不是JSONObject，转换失败:" + object.toString() + ex.getMessage(), ex);
                }
            }
        } else {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("规则名称", "无");
            dataMap.put("级别", "无");
            dataMap.put("规则", "无");
            sheetBuilder.addData(dataMap);
        }

        //空两行
        sheetBuilder.addData(new HashMap<>());
        sheetBuilder.addData(new HashMap<>());

        //指标
        Map<String, Object> fieldsHandMap = new HashMap<>();
        fieldsHandMap.put("规则名称", "指标");
        sheetBuilder.addData(fieldsHandMap);
        Map<String, Object> fieldsDataMap = new HashMap<>();
        fieldsDataMap.put("规则名称", collectionObj.getJSONArray("fields"));
        sheetBuilder.addData(fieldsDataMap);
        return fileName;
    }
}

/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.inspect.api.report;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.InspectStatus;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.inspect.dao.mapper.InspectMapper;
import neatlogic.framework.inspect.dto.InspectResourceVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.binarystream.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.FileUtil;
import neatlogic.framework.util.TimeUtil;
import neatlogic.framework.util.excel.ExcelBuilder;
import neatlogic.framework.util.excel.SheetBuilder;
import neatlogic.module.inspect.service.InspectService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ExportInspectResourceReportApi extends PrivateBinaryStreamApiComponentBase {

    private static Logger logger = LoggerFactory.getLogger(ExportInspectResourceReportApi.class);

    @Resource
    private InspectMapper inspectMapper;

    @Resource
    private InspectService inspectService;

    @Override
    public String getToken() {
        return "inspect/resource/report/export";
    }

    @Override
    public String getName() {
        return "nmiar.exportinspectresourcereportapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "common.keyword"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true, desc = "common.typeid"),
            @Param(name = "protocolIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.protocolidlist"),
            @Param(name = "stateIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.stateidlist"),
            @Param(name = "vendorIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.vendoridlist"),
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.envidlist"),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "term.appsystemidlist"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.appmoduleidlist"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.typeidlist"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, desc = "common.tagidlist"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "term.inspect.inspectstatuslist"),
            @Param(name = "searchField", type = ApiParamType.STRING, desc = "term.cmdb.searchfield"),
            @Param(name = "batchSearchList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.batchsearchlist"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "common.defaultvalue"),
    })
    @Description(desc = "nmiar.exportinspectresourcereportapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        Long typeId = paramObj.getLong("typeId");
        CiVo ciVo = ciCrossoverMapper.getCiById(typeId);
        if (ciVo == null) {
            throw new CiNotFoundException(typeId);
        }
        String fileNameEncode = ciVo.getId() + "_" + ciVo.getLabel() + ".xlsx";
        fileNameEncode = FileUtil.getEncodedFileName(fileNameEncode);
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + fileNameEncode + "\"");

        ExcelBuilder builder = new ExcelBuilder(SXSSFWorkbook.class);
        SheetBuilder sheetBuilder = builder.withBorderColor(HSSFColor.HSSFColorPredefined.GREY_40_PERCENT)
                .withHeadFontColor(HSSFColor.HSSFColorPredefined.WHITE)
                .withHeadBgColor(HSSFColor.HSSFColorPredefined.DARK_BLUE)
                .withColumnWidth(30)
                .addSheet("数据")
                .withHeaderList(getHeaderList())
                .withColumnList(getColumnList());
        Workbook workbook = builder.build();
        List<InspectResourceVo> inspectResourceVoList = null;
        JSONArray defaultValue = paramObj.getJSONArray("defaultValue");
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            inspectResourceVoList = inspectService.getInspectResourceListByIdList(idList);
            for (ResourceVo resourceVo : inspectResourceVoList) {
                Map<String, Object> dataMap = resourceConvertDataMap(resourceVo);
                sheetBuilder.addData(dataMap);
            }
        } else {
            IResourceCenterResourceCrossoverService resourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
            ResourceSearchVo searchVo = resourceCrossoverService.assembleResourceSearchVo(paramObj);
            resourceCrossoverService.handleBatchSearchList(searchVo);
            resourceCrossoverService.setIpFieldAttrIdAndNameFieldAttrId(searchVo);
            int rowNum = inspectService.getInspectResourceCount(searchVo);
            if (rowNum > 0) {
                searchVo.setPageSize(100);
                searchVo.setRowNum(rowNum);
                if (StringUtils.isNotBlank(searchVo.getKeyword())) {
                    int ipKeywordCount = inspectService.getInspectResourceCountByIpKeyword(searchVo);
                    if (ipKeywordCount > 0) {
                        searchVo.setIsIpFieldSort(1);
                    } else {
                        int nameKeywordCount = inspectService.getInspectResourceCountByNameKeyword(searchVo);
                        if (nameKeywordCount > 0) {
                            searchVo.setIsNameFieldSort(1);
                        }
                    }
                }
                for (int i = 1; i <= searchVo.getPageCount(); i++) {
                    searchVo.setCurrentPage(i);
                    List<Long> idList = inspectService.getInspectResourceIdList(searchVo);
                    if (CollectionUtils.isNotEmpty(idList)) {
                        inspectResourceVoList = inspectService.getInspectResourceListByIdList(idList);
                        for (ResourceVo resourceVo : inspectResourceVoList) {
                            Map<String, Object> dataMap = resourceConvertDataMap(resourceVo);
                            sheetBuilder.addData(dataMap);
                        }
                    }
                }
            }
        }
        try (OutputStream os = response.getOutputStream()) {
            workbook.write(os);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 表头信息
     * @return
     */
    private List<String> getHeaderList() {
        List<String> headerList = new ArrayList<>();
        headerList.add("资产id");
        headerList.add("IP地址");
        headerList.add("类型");
        headerList.add("名称");
        headerList.add("巡检状态");
        headerList.add("描述");
        return headerList;
    }

    /**
     * 每列对应的key
     * @return
     */
    private List<String> getColumnList() {
        List<String> columnList = new ArrayList<>();
        columnList.add("resourceId");
        columnList.add("ip:port");
        columnList.add("typeLabel");
        columnList.add("name");
        columnList.add("inspectStatus");
        columnList.add("description");
        return columnList;
    }

    /**
     * 资产对象转换成excel中一行数据dataMap
     * @param resourceVo 资产对象
     */
    private Map<String, Object> resourceConvertDataMap(ResourceVo resourceVo) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("ip:port", resourceVo.getIp() + (resourceVo.getPort() != null ? ":" + resourceVo.getPort() : StringUtils.EMPTY));
        dataMap.put("typeLabel", resourceVo.getTypeLabel());
        dataMap.put("resourceId", resourceVo.getId());
        dataMap.put("name", resourceVo.getName());
        dataMap.put("description", resourceVo.getDescription());
        dataMap.put("inspectStatus", StringUtils.isNotBlank(resourceVo.getInspectStatus()) ? InspectStatus.getText(resourceVo.getInspectStatus()) + " "
                + (resourceVo.getInspectTime() != null ? TimeUtil.convertDateToString(resourceVo.getInspectTime(), TimeUtil.YYYY_MM_DD_HH_MM_SS)
                : StringUtils.EMPTY) : StringUtils.EMPTY);
        return dataMap;
    }
}

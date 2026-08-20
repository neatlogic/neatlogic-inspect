/*
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package neatlogic.module.inspect.service;

import neatlogic.framework.cmdb.crossover.IResourceBuildSqlCrossoverService;
import neatlogic.framework.cmdb.crossover.IResourceEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityConfigVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceQueryCriteriaVo;
import neatlogic.framework.cmdb.utils.ResourceEntityFactory;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.sqlgenerator.$sql;
import neatlogic.framework.sqlgenerator.ExpressionVo;
import neatlogic.framework.sqlgenerator.JoinVo;
import neatlogic.framework.sqlgenerator.SqlVo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class InspectResourceBuildSqlServiceImpl implements InspectResourceBuildSqlService {
    private final Logger logger = LoggerFactory.getLogger(InspectResourceBuildSqlServiceImpl.class);

    @Override
    public String buildGetInspectResourceListByIdListSql(List<Long> idList, List<String> selectFieldNameList) {
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(selectFieldNameList)) {
                selectItemFieldNameList.addAll(selectFieldNameList);
            }
            List<String> filterItemFieldNameList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(idList)) {
                filterItemFieldNameList.add("id");
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            Column idColumn = fieldName2ColumnMap.get("id");
            $sql.addJoin(plainSelect, $sql.join("left join", "autoexec_job_resource_inspect", "ajri").withOn($sql.exp("ajri.resource_id", "=", idColumn.toString())));
            $sql.addJoin(plainSelect, $sql.join("left join", "cmdb_resourcecenter_resource_account", "crra").withOn($sql.exp("crra.resource_id", "=", idColumn.toString())));
            $sql.addJoin(plainSelect, $sql.join("left join", "cmdb_resourcecenter_account", "cra").withOn($sql.exp("cra.id", "=", "crra.account_id")));
            $sql.addJoin(plainSelect, $sql.join("left join", "cmdb_cientity_tag", "crrt").withOn($sql.exp("crrt.cientity_id", "=", idColumn.toString())));
            $sql.addJoin(plainSelect, $sql.join("left join", "autoexec_job_phase_node", "ajpn").withOn($sql.exp(
                    $sql.exp("ajpn.job_phase_id", "=", "ajri.phase_id"),
                    "and",
                    $sql.exp("ajpn.resource_id", "=", idColumn.toString())
            )));
            $sql.addJoin(plainSelect, $sql.join("left join", "cmdb_tag", "ct").withOn($sql.exp("ct.id", "=", "crrt.tag_id")));
            $sql.addSelectColumn(plainSelect, "ajpn.id", "inspectJobPhaseNodeId");
            $sql.addSelectColumn(plainSelect, "ajpn.`job_id`", "jobId");
            $sql.addSelectColumn(plainSelect, "ajpn.status", "jobPhaseNodeStatus");
            $sql.addSelectColumn(plainSelect, "ct.name", "tagName");
            $sql.addWhereExpression(plainSelect, $sql.exp(idColumn.toString(), "in", idList));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectResourceListByIdListSql(List<Long> idList) {
        List<String> fieldNameList = ResourceEntityFactory.getFieldNameListByViewName("scence_ipobject_detail");
        fieldNameList.remove("env_seq_no");
        fieldNameList.remove("vendor_id");
        fieldNameList.remove("vendor_name");
        fieldNameList.remove("vendor_label");
        fieldNameList.remove("datacenter_id");
        fieldNameList.remove("datacenter_name");
        fieldNameList.remove("fcu");
        fieldNameList.remove("fcd");
        fieldNameList.remove("lcu");
        fieldNameList.remove("lcd");
        return buildGetInspectResourceListByIdListSql(idList, fieldNameList);
    }

    @Override
    public String buildGetInspectResourceCountSql(ResourceSearchVo searchVo) {
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = resourceBuildSqlCrossoverService.getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
                filterItemFieldNameList.add(queryCriteriaVo.getSearchField());
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            /*
            <if test="keyword != null and keyword != ''">
                    AND (a.`name` LIKE CONCAT('%', #{keyword}, '%') OR a.`ip` LIKE CONCAT('%', #{keyword}, '%'))
                </if>
             */
            if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
                String keyword = "'%" + queryCriteriaVo.getKeyword() + "%'";
                $sql.addWhereExpression(plainSelect, $sql.exp("(",
                        $sql.exp(fieldName2ColumnMap.get("name").toString(), "like", keyword),
                        "or", $sql.exp(fieldName2ColumnMap.get("ip").toString(), "like", keyword),
                        ")")
                );
            }
            SqlVo sqlVo = getSqlVoForInspect(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            $sql.addSelectColumn(plainSelect, $sql.fun("count", fieldName2ColumnMap.get("id").toString()).withDistinct(true));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectResourceCountByIpKeywordSql(ResourceSearchVo searchVo) {
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = resourceBuildSqlCrossoverService.getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
                filterItemFieldNameList.add(queryCriteriaVo.getSearchField());
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            /*
            <if test="keyword != null and keyword != ''">
                    AND a.`ip` LIKE CONCAT('%', #{keyword}, '%'))
                </if>
             */
            if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
                String keyword = "'%" + queryCriteriaVo.getKeyword() + "%'";
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("ip").toString(), "like", keyword));
            }
            SqlVo sqlVo = getSqlVoForInspect(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            $sql.addSelectColumn(plainSelect, $sql.fun("count", fieldName2ColumnMap.get("id").toString()).withDistinct(true));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectResourceCountByNameKeywordSql(ResourceSearchVo searchVo) {
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = resourceBuildSqlCrossoverService.getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
                filterItemFieldNameList.add(queryCriteriaVo.getSearchField());
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            /*
            <if test="keyword != null and keyword != ''">
                    AND a.`name` LIKE CONCAT('%', #{keyword}, '%'))
                </if>
             */
            if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
                String keyword = "'%" + queryCriteriaVo.getKeyword() + "%'";
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("name").toString(), "like", keyword));
            }
            SqlVo sqlVo = getSqlVoForInspect(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            $sql.addSelectColumn(plainSelect, $sql.fun("count", fieldName2ColumnMap.get("id").toString()).withDistinct(true));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectResourceIdListSql(ResourceSearchVo searchVo) {
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = resourceBuildSqlCrossoverService.getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
                filterItemFieldNameList.add(queryCriteriaVo.getSearchField());
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            /*
            <if test="keyword != null and keyword != ''">
                    AND (a.`name` LIKE CONCAT('%', #{keyword}, '%') OR a.`ip` LIKE CONCAT('%', #{keyword}, '%'))
                </if>
             */
            if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
                String keyword = "'%" + queryCriteriaVo.getKeyword() + "%'";
                $sql.addWhereExpression(plainSelect, $sql.exp("(",
                        $sql.exp(fieldName2ColumnMap.get("name").toString(), "like", keyword),
                        "or", $sql.exp(fieldName2ColumnMap.get("ip").toString(), "like", keyword),
                        ")")
                );
            }
            SqlVo sqlVo = getSqlVoForInspect(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("id").toString());
            $sql.addGroupBy(plainSelect, fieldName2ColumnMap.get("id").toString());
            if (Objects.equals(queryCriteriaVo.getIsNameFieldSort(), 1)) {
                $sql.addOrderBy(plainSelect, $sql.fun("length", fieldName2ColumnMap.get("name").toString()));
            } else if (Objects.equals(queryCriteriaVo.getIsIpFieldSort(), 1)) {
                $sql.addOrderBy(plainSelect, $sql.fun("length", fieldName2ColumnMap.get("ip").toString()));
            }
            $sql.addOrderBy(plainSelect, fieldName2ColumnMap.get("id").toString());
            $sql.setLimit(plainSelect, searchVo.getStartNum(), searchVo.getPageSize());
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectResourceListByIdListAndJobIdSql(List<Long> idList, Long jobId) {
        List<String> fieldNameList = ResourceEntityFactory.getFieldNameListByViewName("scence_ipobject_detail");
        fieldNameList.remove("env_seq_no");
        fieldNameList.remove("vendor_id");
        fieldNameList.remove("vendor_name");
        fieldNameList.remove("vendor_label");
        fieldNameList.remove("datacenter_id");
        fieldNameList.remove("datacenter_name");
        fieldNameList.remove("fcu");
        fieldNameList.remove("fcd");
        fieldNameList.remove("lcu");
        fieldNameList.remove("lcd");

        fieldNameList.remove("monitor_status");
        fieldNameList.remove("monitor_time");
        fieldNameList.remove("inspect_status");
        fieldNameList.remove("inspect_time");
        fieldNameList.remove("maintenance_window");
        fieldNameList.remove("description");
        fieldNameList.remove("app_module_id");
        fieldNameList.remove("app_module_name");
        fieldNameList.remove("app_module_abbr_name");
        fieldNameList.remove("app_system_id");
        fieldNameList.remove("app_system_name");
        fieldNameList.remove("app_system_abbr_name");
        fieldNameList.remove("state_label");
        fieldNameList.remove("env_id");
        fieldNameList.remove("env_name");
        return buildGetInspectResourceListByIdListAndJobIdSql(idList, jobId, fieldNameList);
    }

    @Override
    public String buildGetInspectResourceListByIdListAndJobIdSql(List<Long> idList, Long jobId, List<String> selectFieldNameList) {
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(selectFieldNameList)) {
                selectItemFieldNameList.addAll(selectFieldNameList);
            }
            List<String> filterItemFieldNameList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(idList)) {
                filterItemFieldNameList.add("id");
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            Column idColumn = fieldName2ColumnMap.get("id");
            $sql.addJoin(plainSelect, $sql.join("left join", "autoexec_job_resource_inspect", "ajri").withOn($sql.exp("ajri.resource_id", "=", idColumn.toString())));
            $sql.addJoin(plainSelect, $sql.join("left join", "cmdb_resourcecenter_resource_account", "crra").withOn($sql.exp("crra.resource_id", "=", idColumn.toString())));
            $sql.addJoin(plainSelect, $sql.join("left join", "cmdb_resourcecenter_account", "cra").withOn($sql.exp("cra.id", "=", "crra.account_id")));
            $sql.addJoin(plainSelect, $sql.join("left join", "cmdb_cientity_tag", "crrt").withOn($sql.exp("crrt.cientity_id", "=", idColumn.toString())));
            $sql.addJoin(plainSelect, $sql.join("left join", "autoexec_job_phase_node", "ajpn").withOn($sql.exp("ajpn.resource_id", "=", idColumn.toString())));
            $sql.addJoin(plainSelect, $sql.join("left join", "cmdb_cientity_inspect", "cci").withOn($sql.exp(
                    $sql.exp("cci.ci_entity_id", "=", idColumn.toString()),
                    "and",
                    $sql.exp("cci.job_id", "=", "ajpn.job_id")
            )));
            $sql.addJoin(plainSelect, $sql.join("left join", "cmdb_tag", "ct").withOn($sql.exp("ct.id", "=", "crrt.tag_id")));
            $sql.addSelectColumn(plainSelect, "cci.inspect_status");
            $sql.addSelectColumn(plainSelect, "cci.inspect_time");
            $sql.addSelectColumn(plainSelect, "ajpn.id", "inspectJobPhaseNodeId");
            $sql.addSelectColumn(plainSelect, "ajpn.`job_id`", "jobId");
            $sql.addSelectColumn(plainSelect, "ajpn.status", "jobPhaseNodeStatus");
            $sql.addSelectColumn(plainSelect, "ct.id", "tagId");
            $sql.addSelectColumn(plainSelect, "ct.name", "tagName");
            $sql.addSelectColumn(plainSelect, "ct.description", "tagDescription");
            $sql.addWhereExpression(plainSelect, $sql.exp(idColumn.toString(), "in", idList));
            $sql.addWhereExpression(plainSelect, $sql.exp("ajpn.job_id", "=", jobId));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectAutoexecJobNodeResourceCountSql(ResourceSearchVo searchVo, Long jobId) {
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            queryCriteriaVo.setJobId(jobId);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = resourceBuildSqlCrossoverService.getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
                filterItemFieldNameList.add(queryCriteriaVo.getSearchField());
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            /*
            <if test="keyword != null and keyword != ''">
                    AND (a.`name` LIKE CONCAT('%', #{keyword}, '%') OR a.`ip` LIKE CONCAT('%', #{keyword}, '%'))
                </if>
             */
            if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
                String keyword = "'%" + queryCriteriaVo.getKeyword() + "%'";
                $sql.addWhereExpression(plainSelect, $sql.exp("(",
                        $sql.exp(fieldName2ColumnMap.get("name").toString(), "like", keyword),
                        "or", $sql.exp(fieldName2ColumnMap.get("ip").toString(), "like", keyword),
                        ")")
                );
            }
            SqlVo sqlVo = getSqlVoForInspectConfigFile(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            $sql.addSelectColumn(plainSelect, $sql.fun("count", fieldName2ColumnMap.get("id").toString()).withDistinct(true));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectAutoexecJobNodeResourceCountByIpKeywordSql(ResourceSearchVo searchVo, Long jobId) {
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            queryCriteriaVo.setJobId(jobId);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = resourceBuildSqlCrossoverService.getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
                filterItemFieldNameList.add(queryCriteriaVo.getSearchField());
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            /*
            <if test="keyword != null and keyword != ''">
                    AND a.`ip` LIKE CONCAT('%', #{keyword}, '%'))
                </if>
             */
            if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
                String keyword = "'%" + queryCriteriaVo.getKeyword() + "%'";
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("ip").toString(), "like", keyword));
            }
            SqlVo sqlVo = getSqlVoForInspectConfigFile(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            $sql.addSelectColumn(plainSelect, $sql.fun("count", fieldName2ColumnMap.get("id").toString()).withDistinct(true));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectAutoexecJobNodeResourceCountByNameKeywordSql(ResourceSearchVo searchVo, Long jobId) {
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            queryCriteriaVo.setJobId(jobId);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = resourceBuildSqlCrossoverService.getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
                filterItemFieldNameList.add(queryCriteriaVo.getSearchField());
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            /*
            <if test="keyword != null and keyword != ''">
                    AND a.`name` LIKE CONCAT('%', #{keyword}, '%'))
                </if>
             */
            if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
                String keyword = "'%" + queryCriteriaVo.getKeyword() + "%'";
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("name").toString(), "like", keyword));
            }
            SqlVo sqlVo = getSqlVoForInspectConfigFile(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            $sql.addSelectColumn(plainSelect, $sql.fun("count", fieldName2ColumnMap.get("id").toString()).withDistinct(true));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectAutoexecJobNodeResourceIdListSql(ResourceSearchVo searchVo, Long jobId) {
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            queryCriteriaVo.setJobId(jobId);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = resourceBuildSqlCrossoverService.getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
                filterItemFieldNameList.add(queryCriteriaVo.getSearchField());
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            /*
            <if test="keyword != null and keyword != ''">
                    AND (a.`name` LIKE CONCAT('%', #{keyword}, '%') OR a.`ip` LIKE CONCAT('%', #{keyword}, '%'))
                </if>
             */
            if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
                String keyword = "'%" + queryCriteriaVo.getKeyword() + "%'";
                $sql.addWhereExpression(plainSelect, $sql.exp("(",
                        $sql.exp(fieldName2ColumnMap.get("name").toString(), "like", keyword),
                        "or", $sql.exp(fieldName2ColumnMap.get("ip").toString(), "like", keyword),
                        ")")
                );
            }
            SqlVo sqlVo = getSqlVoForInspectConfigFile(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("id").toString());
            $sql.addGroupBy(plainSelect, fieldName2ColumnMap.get("id").toString());
            if (Objects.equals(queryCriteriaVo.getIsNameFieldSort(), 1)) {
                $sql.addOrderBy(plainSelect, $sql.fun("length", fieldName2ColumnMap.get("name").toString()));
            } else if (Objects.equals(queryCriteriaVo.getIsIpFieldSort(), 1)) {
                $sql.addOrderBy(plainSelect, $sql.fun("length", fieldName2ColumnMap.get("ip").toString()));
            }
            $sql.addOrderBy(plainSelect, fieldName2ColumnMap.get("id").toString());
            $sql.setLimit(plainSelect, searchVo.getStartNum(), searchVo.getPageSize());
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectConfigFileResourceIdListSql(ResourceSearchVo searchVo) {
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = resourceBuildSqlCrossoverService.getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
                filterItemFieldNameList.add(queryCriteriaVo.getSearchField());
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            /*
            <if test="keyword != null and keyword != ''">
                    AND (a.`name` LIKE CONCAT('%', #{keyword}, '%') OR a.`ip` LIKE CONCAT('%', #{keyword}, '%'))
                </if>
             */
            if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
                String keyword = "'%" + queryCriteriaVo.getKeyword() + "%'";
                $sql.addWhereExpression(plainSelect, $sql.exp("(",
                        $sql.exp(fieldName2ColumnMap.get("name").toString(), "like", keyword),
                        "or", $sql.exp(fieldName2ColumnMap.get("ip").toString(), "like", keyword),
                        ")")
                );
            }
            SqlVo sqlVo = getSqlVoForInspect(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            $sql.addJoin(plainSelect, $sql.join("left join", "inspect_config_file_last_change_time", "g").withOn($sql.exp("g.`resource_id`", "=", fieldName2ColumnMap.get("id").toString())));
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("id").toString());
            $sql.addGroupBy(plainSelect, fieldName2ColumnMap.get("id").toString());

            $sql.addOrderBy(plainSelect, $sql.fun("max","g.`last_change_time`"), "desc");
            $sql.addOrderBy(plainSelect, fieldName2ColumnMap.get("id").toString());
            $sql.setLimit(plainSelect, searchVo.getStartNum(), searchVo.getPageSize());
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectConfigFilePathCountSql(ResourceSearchVo searchVo) {
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("name");
            filterItemFieldNameList.add("ip");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            $sql.addJoin(plainSelect, $sql.join("join", "inspect_config_file_path", "b").withOn($sql.exp("b.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            /*
            <if test="keyword != null and keyword != ''">
                AND (a.`path` LIKE CONCAT('%', #{keyword}, '%')
                OR b.`name` LIKE CONCAT('%', #{keyword}, '%')
                OR b.`ip` LIKE CONCAT('%', #{keyword}, '%')
                )
            </if>
             */
            if (StringUtils.isNotBlank(searchVo.getKeyword())) {
                String keyword = "'%" + searchVo.getKeyword() + "%'";
                ExpressionVo orExp = $sql.exp(
                        $sql.exp(fieldName2ColumnMap.get("name").toString(), "like", keyword),
                        "or", $sql.exp(fieldName2ColumnMap.get("ip").toString(), "like", keyword)
                );
                orExp = $sql.exp(orExp, "or", $sql.exp("b.path", "like", keyword));
                $sql.addWhereExpression(plainSelect, $sql.exp("(", orExp, ")")
                );
            }
            /*
            <if test="timeRange != null">
                <if test="timeRange.size() > 0">
                    AND a.`inspect_time` &gt;= STR_TO_DATE(#{timeRange[0]}, '%Y-%m-%d %H:%i:%s')
                </if>
                <if test="timeRange.size() > 1">
                    AND a.`inspect_time` &lt;= STR_TO_DATE(#{timeRange[1]}, '%Y-%m-%d %H:%i:%s')
                </if>
            </if>
             */
            List<String> timeRange = searchVo.getTimeRange();
            if (CollectionUtils.isNotEmpty(timeRange)) {
                $sql.addWhereExpression(plainSelect, $sql.exp("b.inspect_time", ">=", $sql.fun("STR_TO_DATE", $sql.value(timeRange.get(0)), "'%Y-%m-%d %H:%i:%s'")));
                if (timeRange.size() > 1) {
                    $sql.addWhereExpression(plainSelect, $sql.exp("b.inspect_time", "<=", $sql.fun("STR_TO_DATE", $sql.value(timeRange.get(1)), "'%Y-%m-%d %H:%i:%s'")));
                }
            }
            $sql.addSelectColumn(plainSelect, $sql.fun("count", "b.id").withDistinct(true));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectConfigFilePathIdListSql(ResourceSearchVo searchVo) {
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("name");
            filterItemFieldNameList.add("ip");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            $sql.addJoin(plainSelect, $sql.join("join", "inspect_config_file_path", "b").withOn($sql.exp("b.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            /*
            <if test="keyword != null and keyword != ''">
                AND (a.`path` LIKE CONCAT('%', #{keyword}, '%')
                OR b.`name` LIKE CONCAT('%', #{keyword}, '%')
                OR b.`ip` LIKE CONCAT('%', #{keyword}, '%')
                )
            </if>
             */
            if (StringUtils.isNotBlank(searchVo.getKeyword())) {
                String keyword = "'%" + searchVo.getKeyword() + "%'";
                ExpressionVo orExp = $sql.exp(
                        $sql.exp(fieldName2ColumnMap.get("name").toString(), "like", keyword),
                        "or", $sql.exp(fieldName2ColumnMap.get("ip").toString(), "like", keyword)
                );
                orExp = $sql.exp(orExp, "or", $sql.exp("b.path", "like", keyword));
                $sql.addWhereExpression(plainSelect, $sql.exp("(", orExp, ")")
                );
            }
            /*
            <if test="timeRange != null">
                <if test="timeRange.size() > 0">
                    AND a.`inspect_time` &gt;= STR_TO_DATE(#{timeRange[0]}, '%Y-%m-%d %H:%i:%s')
                </if>
                <if test="timeRange.size() > 1">
                    AND a.`inspect_time` &lt;= STR_TO_DATE(#{timeRange[1]}, '%Y-%m-%d %H:%i:%s')
                </if>
            </if>
             */
            List<String> timeRange = searchVo.getTimeRange();
            if (CollectionUtils.isNotEmpty(timeRange)) {
                $sql.addWhereExpression(plainSelect, $sql.exp("b.inspect_time", ">=", $sql.fun("STR_TO_DATE", $sql.value(timeRange.get(0)), "'%Y-%m-%d %H:%i:%s'")));
                if (timeRange.size() > 1) {
                    $sql.addWhereExpression(plainSelect, $sql.exp("b.inspect_time", "<=", $sql.fun("STR_TO_DATE", $sql.value(timeRange.get(1)), "'%Y-%m-%d %H:%i:%s'")));
                }
            }
            $sql.addGroupBy(plainSelect, "b.id");
            $sql.addOrderBy(plainSelect, $sql.fun("max", "b.inspect_time"), "desc");
            $sql.addSelectColumn(plainSelect, "b.id");
            $sql.setLimit(plainSelect, searchVo.getStartNum(), searchVo.getPageSize());
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectConfigFilePathListSql(List<Long> idList) {
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("name");
            filterItemFieldNameList.add("ip");
            filterItemFieldNameList.add("port");
            filterItemFieldNameList.add("type_label");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            $sql.setDistinct(plainSelect, true);
            $sql.addSelectColumn(plainSelect, "b.id", "id");
            $sql.addSelectColumn(plainSelect, "b.resource_id", "resourceId");
            $sql.addSelectColumn(plainSelect, "b.path", "path");
            $sql.addSelectColumn(plainSelect, "b.inspect_time", "inspectTime");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("name").toString(), "resourceName");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("ip").toString(), "resourceIP");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("port").toString(), "resourcePort");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("type_label").toString(), "resourceTypeLabel");
            $sql.addJoin(plainSelect, $sql.join("join", "inspect_config_file_path", "b").withOn($sql.exp("b.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            $sql.addWhereExpression(plainSelect, $sql.exp("b.id", "in", idList));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectConfigFilePathListByJobIdSql(Long jobId) {
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityCrossoverMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = resourceBuildSqlCrossoverService.getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("name");
            filterItemFieldNameList.add("ip");
            filterItemFieldNameList.add("port");
            filterItemFieldNameList.add("type_label");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = resourceBuildSqlCrossoverService.getPlainSelect(config, fieldName2ColumnMap);
            $sql.setDistinct(plainSelect, true);
            $sql.addSelectColumn(plainSelect, "b.id", "id");
            $sql.addSelectColumn(plainSelect, "b.resource_id", "resourceId");
            $sql.addSelectColumn(plainSelect, "b.path", "path");
            $sql.addSelectColumn(plainSelect, "b.md5", "md5");
            $sql.addSelectColumn(plainSelect, "b.inspect_time", "inspectTime");
            $sql.addSelectColumn(plainSelect, "b.file_id", "fileId");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("name").toString(), "resourceName");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("ip").toString(), "resourceIP");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("port").toString(), "resourcePort");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("type_label").toString(), "resourceTypeLabel");
            $sql.addSelectColumn(plainSelect, "c.id", "versionId");
            $sql.addJoin(plainSelect, $sql.join("join", "inspect_config_file_path", "b").withOn($sql.exp("b.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            $sql.addJoin(plainSelect, $sql.join("join", "inspect_config_file_version", "c").withOn($sql.exp(
                    $sql.exp("c.`path_id`", "=", "b.`id`"),
                    "and",
                    $sql.exp("c.`file_id`", "=", "b.`file_id`")
            )));
            $sql.addWhereExpression(plainSelect, $sql.exp("c.job_id", "=", jobId));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
    private SqlVo getSqlVoForInspect(ResourceQueryCriteriaVo queryCriteriaVo, Map<String, Column> fieldName2ColumnMap) {
        SqlVo sqlVo = new SqlVo();
        List<JoinVo> joinList = new ArrayList<>();
        List<ExpressionVo> whereExpressionList = new ArrayList<>();
        /*
        <if test="batchSearchList != null and batchSearchList.size() > 0 and searchField != null and searchField != ''">
            AND
            <if test="searchField == 'name'">
                <foreach collection="batchSearchList" item="item" open="(" separator=" OR " close=")">
                    a.`name` LIKE #{item}
                </foreach>
            </if>
            <if test="searchField == 'ip'">
                <foreach collection="batchSearchList" item="item" open="(" separator=" OR " close=")">
                    a.`ip` LIKE #{item}
                </foreach>
            </if>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
            String columnName = null;
            if (Objects.equals(queryCriteriaVo.getSearchField(), "name")) {
//                System.out.println("b");
                columnName = fieldName2ColumnMap.get("name").toString();
            } else {
//                System.out.println("c");
                columnName = fieldName2ColumnMap.get("ip").toString();
            }
            ExpressionVo orExp = null;
            for (String item : queryCriteriaVo.getBatchSearchList()) {
                if (orExp != null) {
                    orExp = $sql.exp(orExp, "or", $sql.exp(columnName, "like", $sql.value(item)));
                } else {
                    orExp = $sql.exp(columnName, "like", $sql.value(item));
                }
            }
            whereExpressionList.add($sql.exp("(", orExp, ")"));
        }
        /*
        <if test="protocolIdList != null and protocolIdList.size() > 0">
            LEFT JOIN `cmdb_resourcecenter_resource_account` b ON b.`resource_id` = a.`id`
            LEFT JOIN `cmdb_resourcecenter_account` c ON c.`id` = b.`account_id`
        </if>

        <if test="protocolIdList != null and protocolIdList.size() > 0">
            AND c.`protocol_id` IN
            <foreach collection="protocolIdList" item="protocolId" open="(" separator="," close=")">
                #{protocolId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getProtocolIdList())) {
//            System.out.println("d");
            joinList.add($sql.join("left join", "cmdb_resourcecenter_resource_account", "b").withOn($sql.exp("b.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            joinList.add($sql.join("left join", "cmdb_resourcecenter_account", "c").withOn($sql.exp("c.id", "=", "b.account_id")));
            whereExpressionList.add($sql.exp("c.protocol_id", "in", queryCriteriaVo.getProtocolIdList()));
        }
        /*
        <if test="tagIdList != null and tagIdList.size() > 0">
            LEFT JOIN `cmdb_cientity_tag` d ON d.`cientity_id` = a.`id`
        </if>

        <if test="tagIdList != null and tagIdList.size() > 0">
            AND d.`tag_id` IN
            <foreach collection="tagIdList" item="tagId" open="(" separator="," close=")">
                #{tagId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getTagIdList())) {
//            System.out.println("e");
            joinList.add($sql.join("left join", "cmdb_cientity_tag", "d").withOn($sql.exp("d.cientity_id", "=", fieldName2ColumnMap.get("id").toString())));
            whereExpressionList.add($sql.exp("d.tag_id", "in", queryCriteriaVo.getTagIdList()));
        }
        /*
        <if test="inspectJobPhaseNodeStatusList !=null and inspectJobPhaseNodeStatusList.size() > 0">
            left join autoexec_job_resource_inspect ajri on ajri.resource_id=a.id
            left join autoexec_job_phase_node ajpn on ajpn.job_phase_id =ajri.phase_id AND ajpn.resource_id = a.id
        </if>

        <if test="inspectStatusList != null and inspectStatusList.size() > 0">
            AND a.`inspect_status` IN
            <foreach collection="inspectStatusList" item="inspectStatus" open="(" separator="," close=")">
                #{inspectStatus}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getInspectJobPhaseNodeStatusList())) {
//            System.out.println("f");
            joinList.add($sql.join("left join", "autoexec_job_resource_inspect", "ajri").withOn($sql.exp("ajri.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            ExpressionVo expressionVo = $sql.exp($sql.exp("ajpn.job_phase_id", "=", "ajri.phase_id"), "and", $sql.exp("ajpn.resource_id", "=", fieldName2ColumnMap.get("id").toString()));
            joinList.add($sql.join("left join", "autoexec_job_phase_node", "ajpn").withOn(expressionVo));
            whereExpressionList.add($sql.exp("ajpn.status", "in", queryCriteriaVo.getInspectJobPhaseNodeStatusList()));
        }
        /*
        <if test="isHasAuth == false">
            LEFT JOIN cmdb_cientity_group ccg ON ccg.cientity_id = a.id
            LEFT JOIN cmdb_group_auth cga ON ccg.group_id = cga.group_id
             <choose>
                <when test="cmdbGroupType == 'autoexec'">
                    LEFT JOIN cmdb_group cg ON cga.group_id = cg.id AND cg.type in ('autoexec')
                </when>
                <otherwise>
                    LEFT JOIN cmdb_group cg ON cga.group_id = cg.id AND cg.type in ('readonly','maintain','autoexec')
                </otherwise>
            </choose>
        </if>
         */
        if (Objects.equals(queryCriteriaVo.getIsHasAuth(), false)) {
            joinList.add($sql.join("left join", "cmdb_cientity_group", "ccg").withOn($sql.exp("ccg.cientity_id", "=", fieldName2ColumnMap.get("id").toString())));
            joinList.add($sql.join("left join", "cmdb_group_auth", "cga").withOn($sql.exp("cga.group_id", "=", "ccg.group_id")));

            List<String> strList = new ArrayList<>();
            if (Objects.equals(queryCriteriaVo.getCmdbGroupType(), "autoexec")) {
//                System.out.println("g");
                strList.add("autoexec");
            } else {
//                System.out.println("h");
                strList.add("autoexec");
                strList.add("readonly");
                strList.add("maintain");
            }
            ExpressionVo expressionVo = $sql.exp(
                    $sql.exp("cg.id", "=", "cga.group_id"),
                    "and",
                    $sql.exp("cg.type", "in", strList)
            );
            joinList.add($sql.join("left join", "cmdb_group", "cg").withOn(expressionVo));
        }
        /*
         <if test="typeIdList != null and typeIdList.size() > 0">
            <if test="isHasAuth == true">
                AND a.`type_id` IN
                <foreach collection="typeIdList" item="typeId" open="(" separator="," close=")">
                    #{typeId}
                </foreach>
            </if>
            <if test="isHasAuth == false">
                AND (
                <choose>
                    <when test="authedTypeIdList != null and authedTypeIdList.size() >0">
                        a.`type_id` IN
                        <foreach collection="authedTypeIdList" item="authedTypeId" open="(" separator="," close=")">
                            #{authedTypeId}
                        </foreach>
                    </when>
                    <otherwise>
                        1 = 0
                    </otherwise>
                </choose>
                or (
                cg.id is not null and
                a.`type_id` IN
                <foreach collection="typeIdList" item="typeId" open="(" separator="," close=")">
                    #{typeId}
                </foreach>
                and
                ((cga.auth_type = 'common' AND cga.auth_uuid = 'alluser')
                <if test="authenticationInfo != null">
                    OR cga.auth_uuid IN (
                    #{authenticationInfo.userUuid}
                    <if test="authenticationInfo.teamUuidList != null and authenticationInfo.teamUuidList.size() > 0">
                        <foreach collection="authenticationInfo.teamUuidList" item="item" open="," separator=",">
                            #{item}
                        </foreach>
                    </if>
                    <if test="authenticationInfo.roleUuidList != null and authenticationInfo.roleUuidList.size() > 0">
                        <foreach collection="authenticationInfo.roleUuidList" item="item" open="," separator=",">
                            #{item}
                        </foreach>
                    </if>
                )
                </if>
                )
                )
                )
            </if>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getTypeIdList())) {
            if (Objects.equals(queryCriteriaVo.getIsHasAuth(), true)) {
//                System.out.println("i");
                whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("type_id").toString(), "in", queryCriteriaVo.getTypeIdList()));
            } else if (Objects.equals(queryCriteriaVo.getIsHasAuth(), false)) {
                ExpressionVo orLeftExpressionVo = null;
                if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAuthedTypeIdList())) {
//                    System.out.println("j");
                    orLeftExpressionVo = $sql.exp(fieldName2ColumnMap.get("type_id").toString(), "in", queryCriteriaVo.getAuthedTypeIdList());
                } else {
//                    System.out.println("k");
                    orLeftExpressionVo = $sql.exp(1, "=", 0);
                }
                ExpressionVo orRightExpressionVo = $sql.exp($sql.exp("cg.id", "is not null"), "and", $sql.exp(fieldName2ColumnMap.get("type_id").toString(), "in", queryCriteriaVo.getTypeIdList()));

                ExpressionVo orLeftExpressionVo2 = $sql.exp("(", $sql.exp("cga.auth_type", "=", "'common'"), "and", $sql.exp("cga.auth_uuid", "=", "'alluser'"), ")");
                ExpressionVo orRightExpressionVo2 = null;
                if (queryCriteriaVo.getAuthenticationInfo() != null) {
//                    System.out.println("l");
                    List<String> uuidList = new ArrayList<>();
                    if (StringUtils.isNotBlank(queryCriteriaVo.getAuthenticationInfo().getUserUuid())) {
                        uuidList.add(queryCriteriaVo.getAuthenticationInfo().getUserUuid());
                    }
                    if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAuthenticationInfo().getTeamUuidList())) {
                        uuidList.addAll(queryCriteriaVo.getAuthenticationInfo().getTeamUuidList());
                    }
                    if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAuthenticationInfo().getRoleUuidList())) {
                        uuidList.addAll(queryCriteriaVo.getAuthenticationInfo().getRoleUuidList());
                    }
                    if (CollectionUtils.isNotEmpty(uuidList)) {
//                        System.out.println("m");
                        orRightExpressionVo2 = $sql.exp("cga.auth_uuid", "in", uuidList);
                    }
                }
                if (orRightExpressionVo2 != null) {
//                    System.out.println("n");
                    orRightExpressionVo = $sql.exp(orRightExpressionVo, "and", $sql.exp("(", orLeftExpressionVo2, "or", orRightExpressionVo2, ")"));
                } else {
//                    System.out.println("o");
                    orRightExpressionVo = $sql.exp(orRightExpressionVo, "and", orLeftExpressionVo2);
                }
                ExpressionVo orExpressionVo = $sql.exp("(", orLeftExpressionVo, "or", orRightExpressionVo, ")");
                whereExpressionList.add(orExpressionVo);
            }
        }
        /*
        <if test="stateIdList != null and stateIdList.size() > 0">
            AND a.`state_id` IN
            <foreach collection="stateIdList" item="stateId" open="(" separator="," close=")">
                #{stateId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getStateIdList())) {
//            System.out.println("p");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("state_id").toString(), "in", queryCriteriaVo.getStateIdList()));
        }
        /*
        <if test="vendorIdList != null and vendorIdList.size() > 0">
            AND a.`vendor_id` IN
            <foreach collection="vendorIdList" item="vendorId" open="(" separator="," close=")">
                #{vendorId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getVendorIdList())) {
//            System.out.println("q");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("vendor_id").toString(), "in", queryCriteriaVo.getVendorIdList()));
        }
        /*
        <if test="envIdList != null and envIdList.size() > 0">
            AND a.`env_id` IN
            <foreach collection="envIdList" item="envId" open="(" separator="," close=")">
                #{envId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getEnvIdList())) {
//            System.out.println("r");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("env_id").toString(), "in", queryCriteriaVo.getEnvIdList()));
        }
        /*
        <if test="appSystemIdList != null and appSystemIdList.size() > 0">
            AND a.`app_system_id` IN
            <foreach collection="appSystemIdList" item="appSystemId" open="(" separator="," close=")">
                #{appSystemId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAppSystemIdList())) {
//            System.out.println("t");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("app_system_id").toString(), "in", queryCriteriaVo.getAppSystemIdList()));
        }
        /*
        <if test="appModuleIdList != null and appModuleIdList.size() > 0">
            AND a.`app_module_id` IN
            <foreach collection="appModuleIdList" item="appModuleId" open="(" separator="," close=")">
                #{appModuleId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAppModuleIdList())) {
//            System.out.println("u");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("app_module_id").toString(), "in", queryCriteriaVo.getAppModuleIdList()));
        }
        /*
        <if test="inspectStatusList != null and inspectStatusList.size() > 0">
            AND a.`inspect_status` IN
            <foreach collection="inspectStatusList" item="inspectStatus" open="(" separator="," close=")">
                #{inspectStatus}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getInspectStatusList())) {
//            System.out.println("x");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("inspect_status").toString(), "in", queryCriteriaVo.getInspectStatusList()));
        }
        sqlVo.withJoinList(joinList);
        sqlVo.withWhereExpressionList(whereExpressionList);
        return sqlVo;
    }

    private SqlVo getSqlVoForInspectConfigFile(ResourceQueryCriteriaVo queryCriteriaVo, Map<String, Column> fieldName2ColumnMap) {
        SqlVo sqlVo = new SqlVo();
        List<JoinVo> joinList = new ArrayList<>();
        List<ExpressionVo> whereExpressionList = new ArrayList<>();
        /*
        <if test="protocolIdList != null and protocolIdList.size() > 0">
            LEFT JOIN `cmdb_resourcecenter_resource_account` b ON b.`resource_id` = a.`id`
            LEFT JOIN `cmdb_resourcecenter_account` c ON c.`id` = b.`account_id`
        </if>

        <if test="protocolIdList != null and protocolIdList.size() > 0">
            AND c.`protocol_id` IN
            <foreach collection="protocolIdList" item="protocolId" open="(" separator="," close=")">
                #{protocolId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getProtocolIdList())) {
//            System.out.println("d");
            joinList.add($sql.join("left join", "cmdb_resourcecenter_resource_account", "b").withOn($sql.exp("b.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            joinList.add($sql.join("left join", "cmdb_resourcecenter_account", "c").withOn($sql.exp("c.id", "=", "b.account_id")));
            whereExpressionList.add($sql.exp("c.protocol_id", "in", queryCriteriaVo.getProtocolIdList()));
        }
        /*
        <if test="tagIdList != null and tagIdList.size() > 0">
            LEFT JOIN `cmdb_cientity_tag` d ON d.`cientity_id` = a.`id`
        </if>

        <if test="tagIdList != null and tagIdList.size() > 0">
            AND d.`tag_id` IN
            <foreach collection="tagIdList" item="tagId" open="(" separator="," close=")">
                #{tagId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getTagIdList())) {
//            System.out.println("e");
            joinList.add($sql.join("left join", "cmdb_cientity_tag", "d").withOn($sql.exp("d.cientity_id", "=", fieldName2ColumnMap.get("id").toString())));
            whereExpressionList.add($sql.exp("d.tag_id", "in", queryCriteriaVo.getTagIdList()));
        }
        /*
        <if test="inspectJobPhaseNodeStatusList !=null and inspectJobPhaseNodeStatusList.size() > 0">
            left join autoexec_job_resource_inspect ajri on ajri.resource_id=a.id
            left join autoexec_job_phase_node ajpn on ajpn.job_phase_id =ajri.phase_id AND ajpn.resource_id = a.id
        </if>

        <if test="inspectStatusList != null and inspectStatusList.size() > 0">
            AND a.`inspect_status` IN
            <foreach collection="inspectStatusList" item="inspectStatus" open="(" separator="," close=")">
                #{inspectStatus}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getInspectJobPhaseNodeStatusList())) {
//            System.out.println("f");
            joinList.add($sql.join("left join", "autoexec_job_phase_node", "ajpn").withOn($sql.exp("ajpn.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            whereExpressionList.add($sql.exp("ajpn.status", "in", queryCriteriaVo.getInspectJobPhaseNodeStatusList()));
        }
        if (queryCriteriaVo.getJobId() != null) {
            joinList.add($sql.join("left join", "autoexec_job_phase_node", "ajpn").withOn($sql.exp("ajpn.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            whereExpressionList.add($sql.exp("ajpn.job_id", "=", queryCriteriaVo.getJobId()));
        }
        /*
         <if test="typeIdList != null and typeIdList.size() > 0">
            AND a.`type_id` IN
            <foreach collection="typeIdList" item="typeId" open="(" separator="," close=")">
                #{typeId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getTypeIdList())) {
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("type_id").toString(), "in", queryCriteriaVo.getTypeIdList()));
        }
        /*
        <if test="stateIdList != null and stateIdList.size() > 0">
            AND a.`state_id` IN
            <foreach collection="stateIdList" item="stateId" open="(" separator="," close=")">
                #{stateId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getStateIdList())) {
//            System.out.println("p");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("state_id").toString(), "in", queryCriteriaVo.getStateIdList()));
        }
        /*
        <if test="vendorIdList != null and vendorIdList.size() > 0">
            AND a.`vendor_id` IN
            <foreach collection="vendorIdList" item="vendorId" open="(" separator="," close=")">
                #{vendorId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getVendorIdList())) {
//            System.out.println("q");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("vendor_id").toString(), "in", queryCriteriaVo.getVendorIdList()));
        }
        /*
        <if test="envIdList != null and envIdList.size() > 0">
            AND a.`env_id` IN
            <foreach collection="envIdList" item="envId" open="(" separator="," close=")">
                #{envId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getEnvIdList())) {
//            System.out.println("r");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("env_id").toString(), "in", queryCriteriaVo.getEnvIdList()));
        }
        /*
        <if test="appSystemIdList != null and appSystemIdList.size() > 0">
            AND a.`app_system_id` IN
            <foreach collection="appSystemIdList" item="appSystemId" open="(" separator="," close=")">
                #{appSystemId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAppSystemIdList())) {
//            System.out.println("t");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("app_system_id").toString(), "in", queryCriteriaVo.getAppSystemIdList()));
        }
        /*
        <if test="appModuleIdList != null and appModuleIdList.size() > 0">
            AND a.`app_module_id` IN
            <foreach collection="appModuleIdList" item="appModuleId" open="(" separator="," close=")">
                #{appModuleId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAppModuleIdList())) {
//            System.out.println("u");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("app_module_id").toString(), "in", queryCriteriaVo.getAppModuleIdList()));
        }
        /*
        <if test="inspectStatusList != null and inspectStatusList.size() > 0">
            AND a.`inspect_status` IN
            <foreach collection="inspectStatusList" item="inspectStatus" open="(" separator="," close=")">
                #{inspectStatus}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getInspectStatusList())) {
//            System.out.println("x");
            joinList.add($sql.join("left join", "autoexec_job_phase_node", "ajpn").withOn($sql.exp("ajpn.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            joinList.add($sql.join("left join", "cmdb_cientity_inspect", "cci").withOn($sql.exp(
                    $sql.exp("cci.ci_entity_id", "=", fieldName2ColumnMap.get("id").toString()),
                    "and",
                    $sql.exp("cci.job_id", "=", "ajpn.job_id")
            )));
            whereExpressionList.add($sql.exp("cci.inspect_status", "in", queryCriteriaVo.getInspectStatusList()));
        }
        sqlVo.withJoinList(joinList);
        sqlVo.withWhereExpressionList(whereExpressionList);
        return sqlVo;
    }

}

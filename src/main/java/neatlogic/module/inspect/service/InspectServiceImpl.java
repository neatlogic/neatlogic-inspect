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

package neatlogic.module.inspect.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.crossover.IResourceBuildSqlCrossoverService;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.enums.CmdbTenantConfig;
import neatlogic.framework.config.ConfigManager;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.dao.mapper.InspectMapper;
import neatlogic.framework.inspect.dto.InspectConfigFilePathSearchVo;
import neatlogic.framework.inspect.dto.InspectConfigFilePathVo;
import neatlogic.framework.inspect.dto.InspectResourceVo;
import neatlogic.module.inspect.dao.mapper.InspectConfigFileMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class InspectServiceImpl implements InspectService {

    private final Logger logger = LoggerFactory.getLogger(InspectServiceImpl.class);

    private final String MYBATIS_MODE = "mybatis";

    private final String JSQLPARSER_MODE = "jsqlparser";

    private final String COMPARISON_ENABLED = "1";

    @Resource
    private InspectConfigFileMapper inspectConfigFileMapper;

    @Resource
    private InspectMapper inspectMapper;

    @Override
    public List<InspectResourceVo> getInspectResourceListByIdList(List<Long> idList) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        List<InspectResourceVo> newResourceList = new ArrayList<>();
        List<InspectResourceVo> oldResourceList = new ArrayList<>();
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
            String sql = resourceBuildSqlCrossoverService.buildGetInspectResourceListByIdListSql(idList);
            if (StringUtils.isNotBlank(sql)) {
                newResourceList = inspectMapper.getInspectResourceListByIdListSql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldResourceList = inspectMapper.getInspectResourceListByIdList(idList);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            checkInspectResourceListIsEquals(newResourceList, oldResourceList);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newResourceList;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldResourceList;
        }
        return new ArrayList<>();
    }

    @Override
    public List<InspectResourceVo> getInspectResourceListByIdList(List<Long> idList, List<String> selectFieldNameList) {
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        String sql = resourceBuildSqlCrossoverService.buildGetInspectResourceListByIdListSql(idList, selectFieldNameList);
        if (StringUtils.isNotBlank(sql)) {
            return inspectMapper.getInspectResourceListByIdListSql(sql);
        }
        return new ArrayList<>();
    }

    @Override
    public List<InspectResourceVo> getInspectResourceListByIdListAndJobId(List<Long> idList, Long jobId) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        List<InspectResourceVo> newResourceList = new ArrayList<>();
        List<InspectResourceVo> oldResourceList = new ArrayList<>();
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
            String sql = resourceBuildSqlCrossoverService.buildGetInspectResourceListByIdListAndJobIdSql(idList, jobId);
            if (StringUtils.isNotBlank(sql)) {
                newResourceList = inspectMapper.getInspectResourceListByIdListSql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldResourceList = inspectMapper.getInspectResourceListByIdListAndJobId(idList, jobId);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            checkInspectResourceListIsEquals(newResourceList, oldResourceList);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newResourceList;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldResourceList;
        }
        return new ArrayList<>();
    }

    @Override
    public List<InspectResourceVo> getInspectResourceListByIdListAndJobId(List<Long> idList, Long jobId, List<String> selectFieldNameList) {
        IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
        String sql = resourceBuildSqlCrossoverService.buildGetInspectResourceListByIdListAndJobIdSql(idList, jobId, selectFieldNameList);
        if (StringUtils.isNotBlank(sql)) {
            return inspectMapper.getInspectResourceListByIdListSql(sql);
        }
        return new ArrayList<>();
    }

    @Override
    public int getInspectResourceCount(ResourceSearchVo searchVo) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        int newCount = 0;
        int oldCount = 0;
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
            String sql = resourceBuildSqlCrossoverService.buildGetInspectResourceCountSql(searchVo);
            if (StringUtils.isNotBlank(sql)) {
                newCount = resourceCrossoverMapper.getCountBySql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldCount = inspectMapper.getInspectResourceCount(searchVo);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            checkIntIsEquals(newCount, oldCount);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newCount;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldCount;
        }
        return 0;
    }

    @Override
    public int getInspectResourceCountByIpKeyword(ResourceSearchVo searchVo) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        int newCount = 0;
        int oldCount = 0;
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
            String sql = resourceBuildSqlCrossoverService.buildGetInspectResourceCountByIpKeywordSql(searchVo);
            if (StringUtils.isNotBlank(sql)) {
                newCount = resourceCrossoverMapper.getCountBySql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldCount = inspectMapper.getInspectResourceCountByIpKeyword(searchVo);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            checkIntIsEquals(newCount, oldCount);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newCount;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldCount;
        }
        return 0;
    }

    @Override
    public int getInspectResourceCountByNameKeyword(ResourceSearchVo searchVo) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        int newCount = 0;
        int oldCount = 0;
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
            String sql = resourceBuildSqlCrossoverService.buildGetInspectResourceCountByNameKeywordSql(searchVo);
            if (StringUtils.isNotBlank(sql)) {
                newCount = resourceCrossoverMapper.getCountBySql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldCount = inspectMapper.getInspectResourceCountByNameKeyword(searchVo);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            checkIntIsEquals(newCount, oldCount);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newCount;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldCount;
        }
        return 0;
    }

    @Override
    public List<Long> getInspectResourceIdList(ResourceSearchVo searchVo) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        List<Long> newIdList = new ArrayList<>();
        List<Long> oldIdList = new ArrayList<>();
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
            String sql = resourceBuildSqlCrossoverService.buildGetInspectResourceIdListSql(searchVo);
            if (StringUtils.isNotBlank(sql)) {
                newIdList = resourceCrossoverMapper.getIdListBySql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldIdList = inspectMapper.getInspectResourceIdList(searchVo);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            checkLongListIsEquals(newIdList, oldIdList);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newIdList;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldIdList;
        }
        return new ArrayList<>();
    }

    @Override
    public int getInspectAutoexecJobNodeResourceCount(ResourceSearchVo searchVo, Long jobId) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        int newCount = 0;
        int oldCount = 0;
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
            String sql = resourceBuildSqlCrossoverService.buildGetInspectAutoexecJobNodeResourceCountSql(searchVo, jobId);
            if (StringUtils.isNotBlank(sql)) {
                newCount = resourceCrossoverMapper.getCountBySql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldCount = inspectMapper.getInspectAutoexecJobNodeResourceCount(searchVo, jobId);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            checkIntIsEquals(newCount, oldCount);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newCount;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldCount;
        }
        return 0;
    }

    @Override
    public int getInspectAutoexecJobNodeResourceCountByIpKeyword(ResourceSearchVo searchVo, Long jobId) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        int newCount = 0;
        int oldCount = 0;
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
            String sql = resourceBuildSqlCrossoverService.buildGetInspectAutoexecJobNodeResourceCountByIpKeywordSql(searchVo, jobId);
            if (StringUtils.isNotBlank(sql)) {
                newCount = resourceCrossoverMapper.getCountBySql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldCount = inspectMapper.getInspectAutoexecJobNodeResourceCountByIpKeyword(searchVo, jobId);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            checkIntIsEquals(newCount, oldCount);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newCount;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldCount;
        }
        return 0;
    }

    @Override
    public int getInspectAutoexecJobNodeResourceCountByNameKeyword(ResourceSearchVo searchVo, Long jobId) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        int newCount = 0;
        int oldCount = 0;
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
            String sql = resourceBuildSqlCrossoverService.buildGetInspectAutoexecJobNodeResourceCountByNameKeywordSql(searchVo, jobId);
            if (StringUtils.isNotBlank(sql)) {
                newCount = resourceCrossoverMapper.getCountBySql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldCount = inspectMapper.getInspectAutoexecJobNodeResourceCountByNameKeyword(searchVo, jobId);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            checkIntIsEquals(newCount, oldCount);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newCount;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldCount;
        }
        return 0;
    }

    @Override
    public List<Long> getInspectAutoexecJobNodeResourceIdList(ResourceSearchVo searchVo, Long jobId) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        List<Long> newIdList = new ArrayList<>();
        List<Long> oldIdList = new ArrayList<>();
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
            String sql = resourceBuildSqlCrossoverService.buildGetInspectAutoexecJobNodeResourceIdListSql(searchVo, jobId);
            if (StringUtils.isNotBlank(sql)) {
                newIdList = resourceCrossoverMapper.getIdListBySql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldIdList = inspectMapper.getInspectAutoexecJobNodeResourceIdList(searchVo, jobId);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            checkLongListIsEquals(newIdList, oldIdList);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newIdList;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldIdList;
        }
        return new ArrayList<>();
    }

    @Override
    public List<Long> getInspectConfigFileResourceIdList(ResourceSearchVo searchVo) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        List<Long> newIdList = new ArrayList<>();
        List<Long> oldIdList = new ArrayList<>();
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
            String sql = resourceBuildSqlCrossoverService.buildGetInspectConfigFileResourceIdListSql(searchVo);
            if (StringUtils.isNotBlank(sql)) {
                newIdList = resourceCrossoverMapper.getIdListBySql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldIdList = inspectConfigFileMapper.getInspectResourceIdList(searchVo);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            checkLongListIsEquals(newIdList, oldIdList);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newIdList;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldIdList;
        }
        return new ArrayList<>();
    }

    @Override
    public int getInspectConfigFilePathCount(InspectConfigFilePathSearchVo inspectConfigFilePathSearchVo) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        int newCount = 0;
        int oldCount = 0;
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
            ResourceSearchVo searchVo = JSONObject.parseObject(JSONObject.toJSONString(inspectConfigFilePathSearchVo), ResourceSearchVo.class);
            String sql = resourceBuildSqlCrossoverService.buildGetInspectConfigFilePathCountSql(searchVo);
            if (StringUtils.isNotBlank(sql)) {
                newCount = resourceCrossoverMapper.getCountBySql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldCount = inspectConfigFileMapper.getInspectConfigFilePathCount(inspectConfigFilePathSearchVo);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            checkIntIsEquals(newCount, oldCount);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newCount;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldCount;
        }
        return 0;
    }

    @Override
    public List<Long> getInspectConfigFilePathIdList(InspectConfigFilePathSearchVo inspectConfigFilePathSearchVo) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        List<Long> newIdList = new ArrayList<>();
        List<Long> oldIdList = new ArrayList<>();
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
            ResourceSearchVo searchVo = JSONObject.parseObject(JSONObject.toJSONString(inspectConfigFilePathSearchVo), ResourceSearchVo.class);
            String sql = resourceBuildSqlCrossoverService.buildGetInspectConfigFilePathIdListSql(searchVo);
            if (StringUtils.isNotBlank(sql)) {
                newIdList = resourceCrossoverMapper.getIdListBySql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldIdList = inspectConfigFileMapper.getInspectConfigFilePathIdList(inspectConfigFilePathSearchVo);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            checkLongListIsEquals(newIdList, oldIdList);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newIdList;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldIdList;
        }
        return new ArrayList<>();
    }

    @Override
    public List<InspectConfigFilePathVo> getInspectConfigFilePathList(List<Long> idList) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        List<InspectConfigFilePathVo> newResourceList = new ArrayList<>();
        List<InspectConfigFilePathVo> oldResourceList = new ArrayList<>();
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
            String sql = resourceBuildSqlCrossoverService.buildGetInspectConfigFilePathListSql(idList);
            if (StringUtils.isNotBlank(sql)) {
                newResourceList = inspectConfigFileMapper.getInspectConfigFilePathListBySql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldResourceList = inspectConfigFileMapper.getInspectConfigFilePathList(idList);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            checkInspectConfigFilePathListIsEquals(newResourceList, oldResourceList);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newResourceList;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldResourceList;
        }
        return new ArrayList<>();
    }

    @Override
    public List<InspectConfigFilePathVo> getInspectConfigFilePathListByJobId(Long jobId) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        List<InspectConfigFilePathVo> newResourceList = new ArrayList<>();
        List<InspectConfigFilePathVo> oldResourceList = new ArrayList<>();
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
            String sql = resourceBuildSqlCrossoverService.buildGetInspectConfigFilePathListByJobIdSql(jobId);
            if (StringUtils.isNotBlank(sql)) {
                newResourceList = inspectConfigFileMapper.getInspectConfigFilePathListBySql(sql);
            }
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldResourceList = inspectConfigFileMapper.getInspectConfigFilePathListByJobId(jobId);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            checkInspectConfigFilePathListIsEquals(newResourceList, oldResourceList);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newResourceList;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldResourceList;
        }
        return new ArrayList<>();
    }

    private boolean checkLongListIsEquals(List<Long> newIdList, List<Long> oldIdList) {
        if (!Objects.equals(oldIdList, newIdList)) {
            JSONObject resultObj = new JSONObject();
            resultObj.put("idList", newIdList);
            resultObj.put("oldIdList", oldIdList);
            logger.error("资产清单新旧SQL获取结果不一致：{}", resultObj);
            return false;
        }
        return true;
    }

    private boolean checkIntIsEquals(int newCount, int oldCount) {
        if (!Objects.equals(oldCount, newCount)) {
            JSONObject resultObj = new JSONObject();
            resultObj.put("newCount", newCount);
            resultObj.put("oldCount", oldCount);
            logger.error("资产清单新旧SQL获取结果不一致：{}", resultObj);
            return false;
        }
        return true;
    }

    private boolean checkInspectResourceListIsEquals(List<InspectResourceVo> resourceList, List<InspectResourceVo> oldResourceList) {
        if (oldResourceList.size() != resourceList.size()) {
            JSONObject errorObj = new JSONObject();
            errorObj.put("resourceList.size()", resourceList.size());
            errorObj.put("oldResourceList.size()", oldResourceList.size());
            logger.error("资产清单新旧SQL获取tbodyList结果不一致：{}", errorObj);
            return false;
        }
        boolean flag = true;
        resourceList.sort(Comparator.comparing(InspectResourceVo::getId));
        oldResourceList.sort(Comparator.comparing(InspectResourceVo::getId));
        for (int i = 0; i < resourceList.size(); i++) {
            InspectResourceVo resourceVo = resourceList.get(i);
            InspectResourceVo oldResourceVo = oldResourceList.get(i);
            String resourceString = JSONObject.toJSONString(resourceVo);
            String oldResourceString = JSONObject.toJSONString(oldResourceVo);
            if (!Objects.equals(resourceString, oldResourceString)) {
                JSONObject errorObj = new JSONObject();
                errorObj.put("index", i);
                errorObj.put("resourceVo", resourceVo);
                errorObj.put("oldResourceVo", oldResourceVo);
                logger.error("资产清单新旧SQL获取tbodyObj结果不一致：{}", errorObj);
                flag = false;
            }
        }
        return flag;
    }

    private boolean checkInspectConfigFilePathListIsEquals(List<InspectConfigFilePathVo> resourceList, List<InspectConfigFilePathVo> oldResourceList) {
        if (oldResourceList.size() != resourceList.size()) {
            JSONObject errorObj = new JSONObject();
            errorObj.put("resourceList.size()", resourceList.size());
            errorObj.put("oldResourceList.size()", oldResourceList.size());
            logger.error("资产清单新旧SQL获取tbodyList结果不一致：{}", errorObj);
            return false;
        }
        boolean flag = true;
        resourceList.sort(Comparator.comparing(InspectConfigFilePathVo::getId));
        oldResourceList.sort(Comparator.comparing(InspectConfigFilePathVo::getId));
        for (int i = 0; i < resourceList.size(); i++) {
            InspectConfigFilePathVo resourceVo = resourceList.get(i);
            InspectConfigFilePathVo oldResourceVo = oldResourceList.get(i);
            String resourceString = JSONObject.toJSONString(resourceVo);
            String oldResourceString = JSONObject.toJSONString(oldResourceVo);
            if (!Objects.equals(resourceString, oldResourceString)) {
                JSONObject errorObj = new JSONObject();
                errorObj.put("index", i);
                errorObj.put("resourceVo", resourceVo);
                errorObj.put("oldResourceVo", oldResourceVo);
                logger.error("资产清单新旧SQL获取tbodyObj结果不一致：{}", errorObj);
                flag = false;
            }
        }
        return flag;
    }
}

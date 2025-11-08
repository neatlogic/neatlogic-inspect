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

package neatlogic.module.inspect.job.node;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.crossover.IAutoexecJobCrossoverService;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.node.IUpdateNodes;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.IResourceCenterDataSource;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.ResourceCenterDataSourceFactory;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class UpdateNodesByOtherHandler implements IUpdateNodes {

    @Override
    public boolean update(AutoexecCombopExecuteConfigVo executeConfigVo, AutoexecJobVo jobVo, String userName, Long protocolId) {
        boolean isHasNode = false;
        JSONObject otherFilter = executeConfigVo.getExecuteNodeConfig().getOtherFilter();
        if (MapUtils.isNotEmpty(otherFilter)) {
            Long appSystemId = otherFilter.getLong("appSystemId");
            Long appModuleId = otherFilter.getLong("appModuleId");
            Long envId = otherFilter.getLong("envId");
            Long typeId = otherFilter.getLong("typeId");
            JSONArray inspectStatusList = otherFilter.getJSONArray("inspectStatusList");
            ResourceSearchVo searchVo = new ResourceSearchVo();
            searchVo.setAppSystemId(appSystemId);
            searchVo.setAppModuleId(appModuleId);
            searchVo.setEnvId(envId);
            searchVo.setTypeId(typeId);
            searchVo.setInspectStatusList(inspectStatusList.toJavaList(String.class));
            IAutoexecJobCrossoverService autoexecJobCrossoverService = CrossoverServiceFactory.getApi(IAutoexecJobCrossoverService.class);
            IResourceCenterDataSource resourceCenterDataSource = ResourceCenterDataSourceFactory.getResourceCenterDataSource();
            List<ResourceEntityVo> appViewList = resourceCenterDataSource.getAppViewList();
            if (CollectionUtils.isNotEmpty(appViewList)) {
                Set<Long> resourceIdSet = new HashSet<>();
                for (ResourceEntityVo resourceEntityVo : appViewList) {
                    searchVo.setViewName(resourceEntityVo.getName());
                    searchVo.setCurrentPage(1);
                    searchVo.setPageSize(100);
                    List<ResourceVo> resourceList = resourceCenterDataSource.getAppResourceList(searchVo, true);
                    if (CollectionUtils.isNotEmpty(resourceList)) {
                        for (int i = resourceList.size() - 1; i >= 0; i--) {
                            ResourceVo resourceVo = resourceList.get(i);
                            if (resourceVo == null) {
                                resourceList.remove(i);
                                continue;
                            }
                            if (resourceIdSet.contains(resourceVo.getId())) {
                                resourceList.remove(i);
                                continue;
                            }
                            resourceIdSet.add(resourceVo.getId());
                        }
                        if (CollectionUtils.isNotEmpty(resourceList)) {
                            autoexecJobCrossoverService.updateJobPhaseNode(jobVo, resourceList, userName, protocolId);
                            isHasNode = true;
                        }
                        for (int currentPage = 2; currentPage <= searchVo.getPageCount(); currentPage++) {
                            searchVo.setCurrentPage(currentPage);
                            resourceList = resourceCenterDataSource.getAppResourceList(searchVo, true);
                            if (CollectionUtils.isNotEmpty(resourceList)) {
                                for (int i = resourceList.size() - 1; i >= 0; i--) {
                                    ResourceVo resourceVo = resourceList.get(i);
                                    if (resourceVo == null) {
                                        resourceList.remove(i);
                                        continue;
                                    }
                                    if (resourceIdSet.contains(resourceVo.getId())) {
                                        resourceList.remove(i);
                                        continue;
                                    }
                                    resourceIdSet.add(resourceVo.getId());
                                }
                                if (CollectionUtils.isNotEmpty(resourceList)) {
                                    autoexecJobCrossoverService.updateJobPhaseNode(jobVo, resourceList, userName, protocolId);
                                    isHasNode = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return isHasNode;
    }

}

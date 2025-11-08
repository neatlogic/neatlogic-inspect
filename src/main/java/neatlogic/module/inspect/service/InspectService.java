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

import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.inspect.dto.InspectConfigFilePathSearchVo;
import neatlogic.framework.inspect.dto.InspectConfigFilePathVo;
import neatlogic.framework.inspect.dto.InspectResourceVo;

import java.util.List;

public interface InspectService {

    List<InspectResourceVo> getInspectResourceListByIdList(List<Long> idList);

    List<InspectResourceVo> getInspectResourceListByIdList(List<Long> idList, List<String> selectFieldNameList);

    List<InspectResourceVo> getInspectResourceListByIdListAndJobId(List<Long> idList, Long jobId);

    List<InspectResourceVo> getInspectResourceListByIdListAndJobId(List<Long> idList, Long jobId, List<String> selectFieldNameList);

    int getInspectResourceCount(ResourceSearchVo searchVo);

    int getInspectResourceCountByIpKeyword(ResourceSearchVo searchVo);

    int getInspectResourceCountByNameKeyword(ResourceSearchVo searchVo);

    List<Long> getInspectResourceIdList(ResourceSearchVo searchVo);

    int getInspectAutoexecJobNodeResourceCount(ResourceSearchVo searchVo, Long jobId);

    int getInspectAutoexecJobNodeResourceCountByIpKeyword(ResourceSearchVo searchVo, Long jobId);

    int getInspectAutoexecJobNodeResourceCountByNameKeyword(ResourceSearchVo searchVo, Long jobId);

    List<Long> getInspectAutoexecJobNodeResourceIdList(ResourceSearchVo searchVo, Long jobId);

    List<Long> getInspectConfigFileResourceIdList(ResourceSearchVo inspectConfigFilePathSearchVo);

    int getInspectConfigFilePathCount(InspectConfigFilePathSearchVo inspectConfigFilePathSearchVo);

    List<Long> getInspectConfigFilePathIdList(InspectConfigFilePathSearchVo inspectConfigFilePathSearchVo);

    List<InspectConfigFilePathVo> getInspectConfigFilePathList(List<Long> idList);

    List<InspectConfigFilePathVo> getInspectConfigFilePathListByJobId(Long jobId);
}

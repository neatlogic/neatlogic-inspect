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

import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;

import java.util.List;

public interface InspectResourceBuildSqlService {

    String buildGetInspectResourceListByIdListSql(List<Long> idList, List<String> selectFieldNameList);

    String buildGetInspectResourceListByIdListSql(List<Long> idList);

    String buildGetInspectResourceCountSql(ResourceSearchVo searchVo);

    String buildGetInspectResourceCountByIpKeywordSql(ResourceSearchVo searchVo);

    String buildGetInspectResourceCountByNameKeywordSql(ResourceSearchVo searchVo);

    String buildGetInspectResourceIdListSql(ResourceSearchVo searchVo);

    String buildGetInspectAutoexecJobNodeResourceCountSql(ResourceSearchVo searchVo, Long jobId);

    String buildGetInspectAutoexecJobNodeResourceCountByIpKeywordSql(ResourceSearchVo searchVo, Long jobId);

    String buildGetInspectAutoexecJobNodeResourceCountByNameKeywordSql(ResourceSearchVo searchVo, Long jobId);

    String buildGetInspectAutoexecJobNodeResourceIdListSql(ResourceSearchVo searchVo, Long jobId);

    String buildGetInspectResourceListByIdListAndJobIdSql(List<Long> IdList, Long jobId);

    String buildGetInspectResourceListByIdListAndJobIdSql(List<Long> IdList, Long jobId, List<String> selectFieldNameList);

    String buildGetInspectConfigFileResourceIdListSql(ResourceSearchVo searchVo);

    String buildGetInspectConfigFilePathCountSql(ResourceSearchVo searchVo);

    String buildGetInspectConfigFilePathIdListSql(ResourceSearchVo searchVo);

    String buildGetInspectConfigFilePathListSql(List<Long> idList);

    String buildGetInspectConfigFilePathListByJobIdSql(Long jobId);
}

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

package neatlogic.module.inspect.dao.mapper;

import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.inspect.dto.*;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface InspectConfigFileMapper {

    List<InspectConfigFilePathVo> getInspectConfigFilePathListBySql(String sql);

    int getInspectResourceCount(ResourceSearchVo searchVo);

    List<Long> getInspectResourceIdList(ResourceSearchVo searchVo);

    List<InspectResourceVo> getInspectResourceListByIdList(List<Long> idList);

    InspectConfigFilePathVo getInspectConfigFilePathById(Long id);

    List<InspectConfigFilePathVo> getInspectConfigFilePathListByResourceId(Long resourceId);

    List<InspectConfigFilePathVo> getInspectConfigFilePathListByResourceIdList(List<Long> resourceIdList);

    List<InspectConfigFilePathVo> getInspectConfigFileLastChangeTimeListByResourceIdList(List<Long> resourceIdList);

    List<InspectConfigFileVersionVo> getInspectConfigFileVersionListByPathIdList(List<Long> pathIdList);

    int getInspectConfigFileAuditCountByPathId(Long pathId);

    List<Long> getInspectConfigFileAuditIdListByPathId(InspectConfigFileAuditVo searchVo);

    List<InspectConfigFileAuditVo> getInspectConfigFileAuditListByIdList(List<Long> idList);

    int getInspectConfigFileVersionCountByPathId(Long pathId);

    List<InspectConfigFilePathVo> getInspectConfigFileVersionCountByPathIdList(List<Long> pathIdList);

    List<Long> getInspectConfigFileVersionIdListByPathId(InspectConfigFileVersionVo searchVo);

    List<InspectConfigFileVersionVo> getInspectConfigFileVersionListByIdList(List<Long> idList);

    InspectConfigFileVersionVo getInspectConfigFileVersionById(Long id);

    int getInspectConfigFilePathCount(InspectConfigFilePathSearchVo searchVo);

    List<Long> getInspectConfigFilePathIdList(InspectConfigFilePathSearchVo searchVo);

    List<InspectConfigFilePathVo> getInspectConfigFilePathList(List<Long> idList);

    List<InspectConfigFilePathVo> getInspectConfigFilePathListByJobId(Long jobId);

    Long getPreviousVersionIdByPathIdAndVersionId(@Param("pathId") Long pathId, @Param("versionId") Long versionId);

    int insertInspectConfigFilePath(InspectConfigFilePathVo pathVo);

    int insertInspectConfigFileAudit(InspectConfigFileAuditVo auditVo);

    int insertInspectConfigFileVersion(InspectConfigFileVersionVo versionVo);

    int insertInspectConfigFileLastChangeTime(@Param("resourceId") Long resourceId, @Param("lastChangeTime") Date lastChangeTime);

    int updateInspectConfigFilePath(InspectConfigFilePathVo pathVo);

    int resetInspectConfigFilePathFileInfoByIdList(List<Long> idList);

    int deleteInspectConfigFilePathByResourceId(Long resourceId);

    int deleteInspectConfigFilePathById(Long id);

    int deleteInspectConfigFileVersionByPathIdList(List<Long> pathIdList);

    int deleteInspectConfigFileVersionByPathIdAndLEId(InspectConfigFileVersionVo searchVo);

    int deleteInspectConfigFileAuditByPathIdList(List<Long> pathIdList);

    int deleteInspectConfigFileAuditByPathIdAndLEInspectTime(InspectConfigFileAuditVo auditVo);
}

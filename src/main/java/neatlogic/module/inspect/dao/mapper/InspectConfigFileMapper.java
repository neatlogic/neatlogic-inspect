/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.inspect.dao.mapper;

import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.inspect.dto.*;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface InspectConfigFileMapper {

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

    List<InspectConfigFilePathVo> getInspectConfigFilePathList(InspectConfigFilePathSearchVo searchVo);

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

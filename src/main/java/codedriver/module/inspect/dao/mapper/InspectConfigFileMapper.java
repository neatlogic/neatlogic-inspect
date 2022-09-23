/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.dao.mapper;

import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.inspect.dto.*;
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

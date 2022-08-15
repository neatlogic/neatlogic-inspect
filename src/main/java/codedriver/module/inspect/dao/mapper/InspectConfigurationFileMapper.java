/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.dao.mapper;

import codedriver.framework.inspect.dto.InspectResourceConfigurationFilePathVo;
import codedriver.framework.inspect.dto.InspectResourceConfigurationFileRecordVo;
import codedriver.framework.inspect.dto.InspectResourceConfigurationFileVersionVo;

import java.util.List;

public interface InspectConfigurationFileMapper {

    List<String> getPathListByResourceId(Long resourceId);

    InspectResourceConfigurationFilePathVo getInspectResourceConfigurationFilePathById(Long id);

    List<InspectResourceConfigurationFilePathVo> getInspectResourceConfigurationFilePathListByResourceId(Long resourceId);

    List<InspectResourceConfigurationFilePathVo> getInspectResourceConfigurationFilePathListByResourceIdList(List<Long> resourceIdList);

    List<Long> getInspectResourceConfigurationFileRecordIdListByPathIdList(List<Long> pathIdList);

    List<InspectResourceConfigurationFileVersionVo> getInspectResourceConfigurationFileVersionListByPathId(Long pathId);

    List<InspectResourceConfigurationFileVersionVo> getInspectResourceConfigurationFileVersionListByPathIdList(List<Long> pathIdList);

    int getInspectResourceConfigurationFileRecordCountByPathId(Long pathId);

    List<Long> getInspectResourceConfigurationFileRecordIdListByPathId(InspectResourceConfigurationFileRecordVo searchVo);

    List<InspectResourceConfigurationFileRecordVo> getInspectResourceConfigurationFileRecordListByIdList(List<Long> idList);

    int getInspectResourceConfigurationFileVersionCountByPathId(Long pathId);

    List<Long> getInspectResourceConfigurationFileVersionIdListByPathId(InspectResourceConfigurationFileVersionVo searchVo);

    List<InspectResourceConfigurationFileVersionVo> getInspectResourceConfigurationFileVersionListByIdList(List<Long> idList);

    InspectResourceConfigurationFileVersionVo getInspectResourceConfigurationFileVersionById(Long id);

    int insertInspectResourceConfigurationFilePath(InspectResourceConfigurationFilePathVo pathVo);

    int insertInspectResourceConfigurationFileRecord(InspectResourceConfigurationFileRecordVo recordVo);

    void insertInspectResourceConfigurationFileVersion(InspectResourceConfigurationFileVersionVo versionVo);

    int updateInspectResourceConfigurationFilePath(InspectResourceConfigurationFilePathVo pathVo);

    int resetInspectResourceConfigurationFilePathFileInfoById(Long id);

    int resetInspectResourceConfigurationFilePathFileInfoByIdList(List<Long> idList);

    int deleteResourceConfigFilePathByResourceId(Long resourceId);

    int deleteResourceConfigFilePathById(Long id);

    int deleteResourceConfigFilePathByIdList(List<Long> idList);

    int deleteResourceConfigFileVersionByPathId(Long pathId);

    int deleteResourceConfigFileVersionByPathIdList(List<Long> pathIdList);

    int deleteResourceConfigFileRecordByIdList(List<Long> recordIdList);

    int deleteResourceConfigFileRecordByPathId(Long pathId);

    int deleteResourceConfigFileRecordByPathIdList(List<Long> pathIdList);
}

/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.dao.mapper;

import codedriver.framework.inspect.dto.InspectResourceConfigurationFilePathVo;
import codedriver.framework.inspect.dto.InspectResourceConfigurationFileVersionVo;

import java.util.List;

public interface InspectConfigurationFileMapper {

//    List<String> getPathListByResourceId(Long resourceId);

    InspectResourceConfigurationFilePathVo getInpectResourceConfigurationFilePathById(Long id);

    List<InspectResourceConfigurationFilePathVo> getInpectResourceConfigurationFilePathListByResourceId(Long resourceId);

    List<InspectResourceConfigurationFilePathVo> getInpectResourceConfigurationFilePathListByResourceIdList(List<Long> resourceIdList);

//    List<Long> getInpectResourceConfigurationFileRecordIdListByPathId(Long pathId);

//    List<Long> getInpectResourceConfigurationFileRecordIdListByPathIdList(List<Long> pathIdList);

    List<InspectResourceConfigurationFileVersionVo> getInpectResourceConfigurationFileVersionListByPathId(Long pathId);

    List<InspectResourceConfigurationFileVersionVo> getInpectResourceConfigurationFileVersionListByPathIdList(List<Long> pathIdList);

    int insertInpectResourceConfigurationFilePath(InspectResourceConfigurationFilePathVo pathVo);

    int updateInpectResourceConfigurationFilePath(InspectResourceConfigurationFilePathVo pathVo);

    int resetInpectResourceConfigurationFilePathFileInfoById(Long id);

    int resetInpectResourceConfigurationFilePathFileInfoByIdList(List<Long> idList);

    int deleteResourceConfigFilePathByResourceId(Long resourceId);

    int deleteResourceConfigFilePathById(Long id);

//    int deleteResourceConfigFilePathByIdList(List<Long> idList);

    int deleteResourceConfigFileVersionByPathId(Long pathId);
    int deleteResourceConfigFileVersionByPathIdList(List<Long> pathIdList);

//    int deleteResourceConfigFileRecordByIdList(List<Long> recordIdList);

    int deleteResourceConfigFileRecordByPathId(Long pathId);

    int deleteResourceConfigFileRecordByPathIdList(List<Long> pathIdList);
}

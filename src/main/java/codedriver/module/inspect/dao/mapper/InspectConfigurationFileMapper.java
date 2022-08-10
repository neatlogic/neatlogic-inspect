/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.dao.mapper;

import codedriver.framework.inspect.dto.InspectResourceConfigurationFilePathVo;

import java.util.List;

public interface InspectConfigurationFileMapper {

    List<String> getPathListByResourceId(Long resourceId);

    List<InspectResourceConfigurationFilePathVo> getInpectResourceConfigurationFilePathListByResourceId(Long resourceId);

    int insertInpectResourceConfigurationFilePath(InspectResourceConfigurationFilePathVo pathVo);

    int deleteResourceConfigFilePathByResourceId(Long resourceId);

    int deleteResourceConfigFilePathById(Long id);
}

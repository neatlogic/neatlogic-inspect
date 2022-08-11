/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.service;

import codedriver.framework.inspect.dto.InspectResourceConfigurationFilePathVo;

import java.util.List;

public interface InspectConfigurationFileService {
    /**
     * 批量清空配置文件
     * @param resourceIdList 资源id列表
     * @param inspectResourceConfigurationFilePathList 路径列表
     */
    void clearFile(List<Long> resourceIdList, List<InspectResourceConfigurationFilePathVo> inspectResourceConfigurationFilePathList) throws Exception;
}

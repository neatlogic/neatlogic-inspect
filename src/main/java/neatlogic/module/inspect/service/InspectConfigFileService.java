/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
 */

package neatlogic.module.inspect.service;

import neatlogic.framework.inspect.dto.InspectConfigFilePathVo;
import neatlogic.framework.lcs.BaseLineVo;

import java.util.List;

public interface InspectConfigFileService {
    /**
     * 批量清空配置文件
     * @param resourceIdList 资源id列表
     * @param inspectResourceConfigFilePathList 路径列表
     */
    void clearFile(List<Long> resourceIdList, List<InspectConfigFilePathVo> inspectResourceConfigFilePathList) throws Exception;

    /**
     * 根据文件id获取文件内容
     * @param fileId 文件id
     * @return
     * @throws Exception
     */
    List<BaseLineVo> getLineList(Long fileId);
}

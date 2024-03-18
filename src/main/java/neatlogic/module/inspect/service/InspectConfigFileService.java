/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

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

/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.inspect.service;

import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.inspect.dto.InspectConfigFilePathSearchVo;
import neatlogic.framework.inspect.dto.InspectConfigFilePathVo;
import neatlogic.framework.inspect.dto.InspectResourceVo;

import java.util.List;

public interface InspectService {

    List<InspectResourceVo> getInspectResourceListByIdList(List<Long> idList);

    List<InspectResourceVo> getInspectResourceListByIdList(List<Long> idList, List<String> selectFieldNameList);

    List<InspectResourceVo> getInspectResourceListByIdListAndJobId(List<Long> idList, Long jobId);

    List<InspectResourceVo> getInspectResourceListByIdListAndJobId(List<Long> idList, Long jobId, List<String> selectFieldNameList);

    int getInspectResourceCount(ResourceSearchVo searchVo);

    int getInspectResourceCountByIpKeyword(ResourceSearchVo searchVo);

    int getInspectResourceCountByNameKeyword(ResourceSearchVo searchVo);

    List<Long> getInspectResourceIdList(ResourceSearchVo searchVo);

    int getInspectAutoexecJobNodeResourceCount(ResourceSearchVo searchVo, Long jobId);

    int getInspectAutoexecJobNodeResourceCountByIpKeyword(ResourceSearchVo searchVo, Long jobId);

    int getInspectAutoexecJobNodeResourceCountByNameKeyword(ResourceSearchVo searchVo, Long jobId);

    List<Long> getInspectAutoexecJobNodeResourceIdList(ResourceSearchVo searchVo, Long jobId);

    List<Long> getInspectConfigFileResourceIdList(ResourceSearchVo inspectConfigFilePathSearchVo);

    int getInspectConfigFilePathCount(InspectConfigFilePathSearchVo inspectConfigFilePathSearchVo);

    List<Long> getInspectConfigFilePathIdList(InspectConfigFilePathSearchVo inspectConfigFilePathSearchVo);

    List<InspectConfigFilePathVo> getInspectConfigFilePathList(List<Long> idList);

    List<InspectConfigFilePathVo> getInspectConfigFilePathListByJobId(Long jobId);
}

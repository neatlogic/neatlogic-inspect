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

import neatlogic.framework.inspect.dto.InspectNewProblemCustomViewVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface InspectNewProblemCustomViewMapper {

    int checkInspectNewProblemCustomViewIsExists(InspectNewProblemCustomViewVo vo);

    InspectNewProblemCustomViewVo getInspectNewProblemCustomViewById(Long id);

    List<InspectNewProblemCustomViewVo> getInspectNewProblemCustomViewListByUserUuid(String userUuid);

    Integer getMaxSortByUserUuid(String userUuid);

    int updateInspectNewProblemCustomViewName(InspectNewProblemCustomViewVo vo);

    int updateInspectNewProblemCustomViewCondition(InspectNewProblemCustomViewVo vo);

    int updateInspectNewProblemCustomViewSort(InspectNewProblemCustomViewVo vo);

    int updateSortDecrement(@Param("userUuid") String userUuid, @Param("fromSort") Integer fromSort, @Param("toSort") Integer toSort);

    int updateSortIncrement(@Param("userUuid") String userUuid, @Param("fromSort") Integer fromSort, @Param("toSort") Integer toSort);

    int insertInspectNewProblemCustomView(InspectNewProblemCustomViewVo vo);

    int deleteInspectNewProblemCustomViewById(Long id);
}

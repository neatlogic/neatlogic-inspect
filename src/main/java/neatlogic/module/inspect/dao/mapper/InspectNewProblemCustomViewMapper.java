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

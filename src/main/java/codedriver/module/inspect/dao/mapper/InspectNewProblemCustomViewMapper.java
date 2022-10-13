/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.dao.mapper;

import codedriver.framework.inspect.dto.InspectNewProblemCustomViewVo;
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

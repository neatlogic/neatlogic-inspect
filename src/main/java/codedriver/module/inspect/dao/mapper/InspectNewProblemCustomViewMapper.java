/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.dao.mapper;

import codedriver.framework.inspect.dto.InspectNewProblemCustomViewVo;

import java.util.List;

public interface InspectNewProblemCustomViewMapper {

    int checkInspectNewProblemCustomViewIsExists(InspectNewProblemCustomViewVo vo);

    InspectNewProblemCustomViewVo getInspectNewProblemCustomViewById(Long id);

    List<InspectNewProblemCustomViewVo> getInspectNewProblemCustomViewListByUserUuid(String userUuid);

    int updateInspectNewProblemCustomView(InspectNewProblemCustomViewVo vo);

    int updateInspectNewProblemCustomViewName(InspectNewProblemCustomViewVo vo);

    int updateInspectNewProblemCustomViewCondition(InspectNewProblemCustomViewVo vo);

    int insertInspectNewProblemCustomView(InspectNewProblemCustomViewVo vo);

    int deleteInspectNewProblemCustomViewById(Long id);
}

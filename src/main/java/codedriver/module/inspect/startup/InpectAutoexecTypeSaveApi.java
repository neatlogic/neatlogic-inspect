/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.startup;

import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.AutoexecTypeVo;
import codedriver.framework.common.constvalue.SystemUser;
import codedriver.framework.startup.IStartup;
import codedriver.framework.startup.StartupBase;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
class InspectAutoexecTypeSaveApi extends StartupBase {

    @Resource

    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getName() {
        return "添加巡检的工具分类";
    }

    @Override
    public void executeForCurrentTenant() {
        AutoexecTypeVo typeVo = new AutoexecTypeVo();
        typeVo.setId(1L);
        typeVo.setDescription("巡检");
        typeVo.setName("INSPECTION");
        typeVo.setLcu(SystemUser.SYSTEM.getUserUuid());
        autoexecTypeMapper.replaceType(typeVo);
    }

    @Override
    public void executeForAllTenant() {

    }

    @Override
    public int sort() {
        return 0;
    }
}

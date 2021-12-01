package codedriver.module.inspect.api;

import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.AutoexecTypeVo;
import codedriver.framework.startup.IStartup;

import javax.annotation.Resource;

public class InpectAutoexecTypeSaveApi implements IStartup {

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getName() {
        return "添加巡检的工具分类";
    }

    @Override
    public void executeForCurrentTenant() {
        AutoexecTypeVo typeVo = new AutoexecTypeVo();
        typeVo.setId(445566778899L);
        typeVo.setDescription("巡检");
        typeVo.setName("INSPECTION");
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

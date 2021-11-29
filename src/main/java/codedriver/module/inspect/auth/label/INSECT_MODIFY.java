package codedriver.module.inspect.auth.label;

import codedriver.framework.auth.core.AuthBase;

import java.util.Collections;
import java.util.List;

public class INSECT_MODIFY extends AuthBase {
    @Override
    public String getAuthDisplayName() {
        return "巡检管理员权限";
    }

    @Override
    public String getAuthIntroduction() {
        return "设置阈值规则，预定义模型对应的巡检工具";
    }

    @Override
    public String getAuthGroup() {
        return "inspect";
    }

    @Override
    public Integer getSort() {
        return 1;
    }

    @Override
    public List<Class<? extends AuthBase>> getIncludeAuths(){
        return Collections.singletonList(INSPECT_BASE.class);
    }
}

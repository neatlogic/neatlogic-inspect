package codedriver.module.inspect.auth.label;

import codedriver.framework.auth.core.AuthBase;

public class INSPECT_BASE extends AuthBase {
    @Override
    public String getAuthDisplayName() {
        return "巡检基础权限";
    }

    @Override
    public String getAuthIntroduction() {
        return "可以设置定时巡检和定时批量巡检";
    }

    @Override
    public String getAuthGroup() {
        return "inspect";
    }

    @Override
    public Integer getSort() {
        return 2;
    }
}

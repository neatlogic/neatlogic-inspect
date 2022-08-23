/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.file;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.file.core.FileTypeHandlerBase;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class InspectConfigFileHandler extends FileTypeHandlerBase {
    @Override
    protected boolean myDeleteFile(FileVo fileVo, JSONObject paramObj) {
        return true;
    }

    @Override
    public boolean valid(String userUuid, FileVo fileVo, JSONObject jsonObj) throws Exception {
        return AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), INSPECT_BASE.class.getSimpleName());
    }

    @Override
    public String getName() {
        return "INSPECTCONFIGFILE";
    }

    @Override
    public String getDisplayName() {
        return "巡检资源配置文件备份";
    }
}

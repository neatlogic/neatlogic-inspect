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

package neatlogic.module.inspect.file;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.file.core.FileTypeHandlerBase;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
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

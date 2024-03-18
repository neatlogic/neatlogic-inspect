/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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
package neatlogic.module.inspect.api.definition;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.MongoCollection;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.service.InspectCollectService;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author longrf
 * @date 2022/11/18 17:20
 */

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveInspectAppCollectionThresholdsApi extends PrivateApiComponentBase {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private InspectCollectService inspectCollectService;

    @Override
    public String getName() {
        return "保存应用巡检阈值设置";
    }

    @Override
    public String getToken() {
        return "inspect/app/collection/thresholds/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "模型名称（唯一标识）"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用id"),
            @Param(name = "isOverWrite", type = ApiParamType.INTEGER, isRequired = true, desc = "是否覆盖(0不覆盖，1覆盖)"),
            @Param(name = "thresholds", type = ApiParamType.JSONARRAY, desc = "集合数据定义")})
    @Description(desc = "保存应用巡检阈值设置，需要依赖mongodb")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        String name = paramObj.getString("name");
        JSONArray thresholds = paramObj.getJSONArray("thresholds");
        //校验阈值规则参数
        inspectCollectService.checkThresholdsParam(thresholds);

        //校验应用id是否存在
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        CiEntityVo appSystemCiEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(appSystemId);
        if (appSystemCiEntity == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("appSystemId"));
        }

        MongoCollection<Document> defAppCollection = mongoTemplate.getCollection("_inspectdef_app");
        Document whereDoc = new Document();
        whereDoc.put("appSystemId", appSystemId);
        whereDoc.put("name", name);

        IAppSystemMapper iAppSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        AppSystemVo appSystemVo = iAppSystemMapper.getAppSystemById(appSystemId);

        if (defAppCollection.find(whereDoc).first() != null) {
            Document updateDoc = new Document();
            Document setDocument = new Document();
            updateDoc.put("thresholds", thresholds);
            updateDoc.put("isOverWrite", paramObj.getInteger("isOverWrite"));
            updateDoc.put("lcu", UserContext.get().getUserUuid());
            updateDoc.put("lcd", new Date());
            updateDoc.put("appSystemName", appSystemVo.getName());
            updateDoc.put("appSystemAbbrName", appSystemVo.getAbbrName());
            setDocument.put("$set", updateDoc);
            mongoTemplate.getCollection("_inspectdef_app").updateOne(whereDoc, setDocument);
        } else {
            Document newDoc = new Document();
            newDoc.put("appSystemId", appSystemId);
            newDoc.put("name", name);
            newDoc.put("thresholds", thresholds);
            newDoc.put("isOverWrite", paramObj.getInteger("isOverWrite"));
            newDoc.put("lcu", UserContext.get().getUserUuid());
            newDoc.put("lcd", new Date());
            newDoc.put("appSystemName", appSystemVo.getName());
            newDoc.put("appSystemAbbrName", appSystemVo.getAbbrName());
            defAppCollection.insertOne(newDoc);
        }
        return null;
    }
}

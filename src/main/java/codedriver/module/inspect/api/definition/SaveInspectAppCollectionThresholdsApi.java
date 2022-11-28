/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.inspect.api.definition;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.IAppSystemMapper;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.auth.INSPECT_MODIFY;
import codedriver.framework.inspect.exception.InspectDefLessLevelException;
import codedriver.framework.inspect.exception.InspectDefLessNameException;
import codedriver.framework.inspect.exception.InspectDefLessRuleException;
import codedriver.framework.inspect.exception.InspectDefRoleNameRepeatException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author longrf
 * @date 2022/11/18 17:20
 */

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveInspectAppCollectionThresholdsApi extends PrivateApiComponentBase {

    @Resource
    private MongoTemplate mongoTemplate;

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
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "集合名称（唯一标识）"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用id"),
            @Param(name = "thresholds", type = ApiParamType.JSONARRAY, desc = "集合数据定义")})
    @Description(desc = "保存应用巡检阈值设置，需要依赖mongodb")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        String name = paramObj.getString("name");
        JSONArray thresholds = paramObj.getJSONArray("thresholds");
        if (!CollectionUtils.isEmpty(thresholds)) {
            List<String> nameList = new ArrayList<>();
            for (int i = 0; i < thresholds.size(); i++) {
                JSONObject thresholdTmp = thresholds.getJSONObject(i);
                if (!thresholdTmp.containsKey("name")) {
                    throw new InspectDefLessNameException(i);
                }
                if (!thresholdTmp.containsKey("level")) {
                    throw new InspectDefLessLevelException(i);
                }
                if (!thresholdTmp.containsKey("rule")) {
                    throw new InspectDefLessRuleException(i);
                }

                //判断name是否重复
                String nameTmp = thresholdTmp.getString("name");
                if (nameList.contains(nameTmp)) {
                    throw new InspectDefRoleNameRepeatException(nameTmp);
                }
                nameList.add(thresholdTmp.getString("name"));
            }
        }

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
            updateDoc.put("lcu", UserContext.get().getUserUuid());
            updateDoc.put("lcd", new Date());
            updateDoc.put("appSystemName", appSystemVo.getName());
            updateDoc.put("appSystemAbbrName",appSystemVo.getAbbrName());
            setDocument.put("$set", updateDoc);
            mongoTemplate.getCollection("_inspectdef_app").updateOne(whereDoc, setDocument);
        } else {
            Document newDoc = new Document();
            newDoc.put("appSystemId", appSystemId);
            newDoc.put("name", name);
            newDoc.put("thresholds", thresholds);
            newDoc.put("lcu", UserContext.get().getUserUuid());
            newDoc.put("lcd", new Date());
            newDoc.put("appSystemName", appSystemVo.getName());
            newDoc.put("appSystemAbbrName",appSystemVo.getAbbrName());
            defAppCollection.insertOne(newDoc);
        }
        return null;
    }
}

/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.inspect.api.definition;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.IAppSystemMapper;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.cmdb.exception.sync.CollectionNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.auth.INSPECT_MODIFY;
import codedriver.framework.inspect.exception.InspectAppThresholdsCopyTargetAppSystemNotFoundException;
import codedriver.framework.inspect.exception.InspectAppThresholdsNotFoundException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.MongoCollection;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/11/23 11:38
 */

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class CopyInspectAppCollectionThresholdsApi extends PrivateApiComponentBase {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "复制应用巡检阈值设置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "inspect/app/collection/thresholds/copy";
    }

    @Input({
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "模型名称（唯一标识）"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用id"),
            @Param(name = "targetAppSystemIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "应用id")
    })
    @Description(desc = "复制应用巡检阈值设置，需要依赖mongodb")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        IAppSystemMapper iAppSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        AppSystemVo appSystemVo = iAppSystemMapper.getAppSystemById(appSystemId);
        if (appSystemVo == null) {
            throw new CiEntityNotFoundException(appSystemId);
        }

        List<Long> targetAppSystemIdList = paramObj.getJSONArray("targetAppSystemIdList").toJavaList(Long.class);
        List<AppSystemVo> targetAppSystemList = iAppSystemMapper.getAppSystemListByIdList(targetAppSystemIdList);
        if (CollectionUtils.isEmpty(targetAppSystemList)) {
            throw new InspectAppThresholdsCopyTargetAppSystemNotFoundException();
        }

        //获取应用个性化阈值
        String name = paramObj.getString("name");
        Document searchDoc = new Document();
        Document returnDoc = new Document();
        searchDoc.put("name", name);
        searchDoc.put("appSystemId", appSystemId);
        returnDoc.put("thresholds", true);
        MongoCollection<Document> defAppCollection = mongoTemplate.getDb().getCollection("_inspectdef_app");
        Document defAppDoc = defAppCollection.find(searchDoc).projection(returnDoc).first();
        if (defAppDoc == null) {
            throw new CollectionNotFoundException("_inspectdef_app");
        }

        JSONArray defAppThresholds = JSONObject.parseObject(defAppDoc.toJson()).getJSONArray("thresholds");
        if (CollectionUtils.isEmpty(defAppThresholds)) {
            return new InspectAppThresholdsNotFoundException(appSystemId, name);
        }

        Map<Long, AppSystemVo> existTargetAppSystemVoMap = targetAppSystemList.stream().collect(Collectors.toMap(AppSystemVo::getId, e -> e));
        for (Long targetAppSystemId : targetAppSystemIdList) {
            //目标系统已不存在，将不会复制个性化阈值
            if (!existTargetAppSystemVoMap.containsKey(targetAppSystemId)) {
                continue;
            }

            Document whereDoc = new Document();
            whereDoc.put("appSystemId", targetAppSystemId);
            whereDoc.put("name", name);
            if (defAppCollection.find(whereDoc).first() != null) {
                Document updateDoc = new Document();
                Document setDocument = new Document();
                updateDoc.put("thresholds", defAppThresholds);
                updateDoc.put("lcu", UserContext.get().getUserUuid());
                updateDoc.put("lcd", new Date());
                updateDoc.put("appSystemName", existTargetAppSystemVoMap.get(targetAppSystemId).getName());
                updateDoc.put("appSystemAbbrName", existTargetAppSystemVoMap.get(targetAppSystemId).getAbbrName());
                setDocument.put("$set", updateDoc);
                defAppCollection.updateOne(whereDoc, setDocument);
            } else {
                Document newDoc = new Document();
                newDoc.put("appSystemId", targetAppSystemId);
                newDoc.put("name", name);
                newDoc.put("thresholds", defAppThresholds);
                newDoc.put("lcu", UserContext.get().getUserUuid());
                newDoc.put("lcd", new Date());
                newDoc.put("appSystemName", existTargetAppSystemVoMap.get(targetAppSystemId).getName());
                newDoc.put("appSystemAbbrName", existTargetAppSystemVoMap.get(targetAppSystemId).getAbbrName());
                defAppCollection.insertOne(newDoc);
            }
        }
        return null;
    }
}
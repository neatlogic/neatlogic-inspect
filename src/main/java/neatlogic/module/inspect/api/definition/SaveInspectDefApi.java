package neatlogic.module.inspect.api.definition;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.inspect.auth.INSPECT_MODIFY;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.inspect.service.InspectCollectService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

@Service
@Transactional
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveInspectDefApi extends PrivateApiComponentBase {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Resource
    private InspectCollectService inspectCollectService;

    @Override
    public String getName() {
        return "保存巡检规则";
    }

    @Override
    public String getToken() {
        return "inspect/collection/def/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "模型名称（唯一标识）"),
            @Param(name = "thresholds", type = ApiParamType.JSONARRAY, desc = "集合数据定义"),
            @Param(name = "deletedRuleUuidList", type = ApiParamType.JSONARRAY, desc = "删除的规则序号列表")
    })
    @Description(desc = "保存巡检规则接口，用于巡检模块的巡检规则保存，需要依赖mongodb")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String name = paramObj.getString("name");
        JSONArray thresholds = paramObj.getJSONArray("thresholds");
        //校验阈值规则参数
        inspectCollectService.checkThresholdsParam(thresholds);
        Document whereDoc = new Document();
        Document updateDoc = new Document();
        Document setDocument = new Document();
        whereDoc.put("name", name);
        updateDoc.put("thresholds", thresholds);
        updateDoc.put("lcu", UserContext.get().getUserUuid());
        updateDoc.put("lcd", new Date());
        setDocument.put("$set", updateDoc);
        mongoTemplate.getCollection("_inspectdef").updateOne(whereDoc, setDocument);

        /*
         1、找出此模型并且含有重写规则的个性化阈值列表
         2、删除rule
         */
        JSONArray deletedRuleUuidArray = paramObj.getJSONArray("deletedRuleUuidList");
        if (CollectionUtils.isNotEmpty(deletedRuleUuidArray)) {

            //1、找出此模型并且含有重写规则的个性化阈值列表
            Document returnDoc = new Document();
            Document searchDoc = new Document();
            searchDoc.put("name", paramObj.getString("name"));
            searchDoc.put("isOverWrite", 1);
            returnDoc.put("thresholds", true);
            returnDoc.put("appSystemId", true);
            MongoCollection<Document> defAppCollection = mongoTemplate.getDb().getCollection("_inspectdef_app");
            FindIterable<Document> documents = defAppCollection.find(searchDoc).projection(returnDoc);

            //2、删除rule
            if (documents.first() != null) {
                if (CollectionUtils.isNotEmpty(deletedRuleUuidArray)) {
                    for (Document document : documents) {
                        JSONObject appDefJson = JSONObject.parseObject(document.toJson());
                        Long appSystemId = appDefJson.getLong("appSystemId");
                        JSONArray docThresholds = appDefJson.getJSONArray("thresholds");
                        if (appSystemId == null || CollectionUtils.isEmpty(docThresholds)) {
                            continue;
                        }
                        //重写的规则标志
                        boolean overwriteFlag = false;
                        //需要删除的规则
                        JSONArray removeList = new JSONArray();
                        for (Object object : docThresholds) {
                            JSONObject jsonObject = (JSONObject) object;
                            String ruleUuid = jsonObject.getString("ruleUuid");
                            if (StringUtils.isEmpty(ruleUuid)) {
                                removeList.add(object);
                                continue;
                            }
                            if (deletedRuleUuidArray.contains(ruleUuid)) {
                                removeList.add(object);
                            } else if (jsonObject.containsKey("isOverWrite") && jsonObject.getInteger("isOverWrite") == 1 && !overwriteFlag) {
                                overwriteFlag = true;
                            }
                        }
                        //重写的规则标志为false表示：没有重写的规则
                        if (!overwriteFlag) {
                            appDefJson.put("isOverWrite", 0);
                        }
                        if (CollectionUtils.isNotEmpty(removeList)) {
                            docThresholds.removeAll(removeList);
                            Document whereAppDoc = new Document();
                            whereAppDoc.put("name", name);
                            whereAppDoc.put("appSystemId", appSystemId);
                            Document updateAppDoc = new Document();
                            Document setAppDoc = new Document();
                            updateAppDoc.put("thresholds", docThresholds);
                            if (appDefJson.containsKey("isOverWrite")) {
                                updateAppDoc.put("isOverWrite", appDefJson.getInteger("isOverWrite"));
                            }
                            setAppDoc.put("$set", updateAppDoc);
                            defAppCollection.updateOne(whereAppDoc, setAppDoc);
                        }
                    }
                }
            }
        }
        return null;
    }
}

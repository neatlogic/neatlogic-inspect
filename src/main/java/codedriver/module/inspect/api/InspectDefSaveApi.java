package codedriver.module.inspect.api;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
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
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class InspectDefSaveApi extends PrivateApiComponentBase {

    @Autowired
    private MongoTemplate mongoTemplate;

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
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "唯一标识"),
            @Param(name = "thresholds", type = ApiParamType.JSONARRAY, desc = "集合数据定义")})
    @Description(desc = "保存巡检规则接口，用于巡检模块的巡检规则保存，需要依赖mongodb")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String name = paramObj.getString("name");
        JSONArray thresholds = paramObj.getJSONArray("thresholds");
        if (!CollectionUtils.isEmpty(thresholds)) {
            List<String> nameList = new ArrayList<>();
            for (int i = 0; i < thresholds.size(); i++) {
                JSONObject thresholdTmp = thresholds.getJSONObject(i);
                if (!thresholdTmp.containsKey("name")) {
                    throw new InspectDefLessNameException();
                }
                if (!thresholdTmp.containsKey("level")) {
                    throw new InspectDefLessLevelException();
                }
                if (!thresholdTmp.containsKey("rule")) {
                    throw new InspectDefLessRuleException();
                }

                //判断name是否重复
                String nameTmp = thresholdTmp.getString("name");
                if (nameList.contains(nameTmp)) {
                    throw new InspectDefRoleNameRepeatException(nameTmp);
                }
                nameList.add(thresholdTmp.getString("name"));
            }
        }
        Document whereDoc = new Document();
        Document updateDoc = new Document();
        Document setDocument = new Document();
        whereDoc.put("name", name);
        updateDoc.put("thresholds", thresholds);
        updateDoc.put("lcu", UserContext.get().getUserUuid());
        updateDoc.put("lcd", new Date());
        setDocument.put("$set", updateDoc);
        mongoTemplate.getCollection("_inspectdef").updateOne(whereDoc, setDocument);
        return null;
    }
}

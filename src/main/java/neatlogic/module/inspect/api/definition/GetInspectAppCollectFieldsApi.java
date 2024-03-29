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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import neatlogic.framework.cmdb.exception.sync.CollectionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author longrf
 * @date 2022/11/18 18:24
 */

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetInspectAppCollectFieldsApi extends PrivateApiComponentBase {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private UserMapper userMapper;

    @Override
    public String getName() {
        return "获取应用巡检数据结构和阈值规则";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "inspect/app/collection/fields/get";
    }

    @Input({
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "模型名称（唯一标识）"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用id")
    })
    @Output({
            @Param(name = "appSystemName", type = ApiParamType.LONG, desc = "应用系统名"),
            @Param(name = "appSystemAbbrName", type = ApiParamType.LONG, desc = "应用系统简称"),
            @Param(name = "label", type = ApiParamType.STRING, desc = "名称"),
            @Param(name = "fields", type = ApiParamType.LONG, desc = "数据结构列表"),
            @Param(name = "globalThresholds", type = ApiParamType.LONG, desc = "全局阈值规则列表"),
            @Param(name = "appThresholds", type = ApiParamType.LONG, desc = "应用层个性化阈值规则列表"),
            @Param(name = "userVo", explode = UserVo.class, desc = "修改人"),
            @Param(name = "lcd", type = ApiParamType.LONG, desc = "修改时间戳")
    })
    @Description(desc = "获取应用巡检数据结构和阈值规则，需要依赖mongodb")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject returnObject = new JSONObject();
        JSONArray returnFieldsArray = new JSONArray();

        //获取字典数据结构
        JSONObject dictionary = mongoTemplate.findOne(new Query(Criteria.where("name").is(paramObj.getString("name"))), JSONObject.class, "_dictionary");
        if (dictionary == null) {
            throw new CollectionNotFoundException("_dictionary");
        }
        JSONArray dictionaryArray = dictionary.getJSONArray("fields");
        if (CollectionUtils.isEmpty(dictionaryArray)) {
            return returnObject;
        }

        IAppSystemMapper iAppSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        AppSystemVo appSystemVo = iAppSystemMapper.getAppSystemById(paramObj.getLong("appSystemId"));
        if (appSystemVo == null) {
            return null;
        }
        returnObject.put("appSystemName", appSystemVo.getName());
        returnObject.put("appSystemAbbrName", appSystemVo.getAbbrName());
        returnObject.put("label", dictionary.getString("label"));

        //获取顶层数据结构的过滤条件和顶层规则 fields、thresholds
        Document searchDoc = new Document();
        Document fieldDocument = new Document();
        searchDoc.put("name", paramObj.getString("name"));
        fieldDocument.put("fields", true);
        fieldDocument.put("thresholds", true);
        Document defDoc = mongoTemplate.getDb().getCollection("_inspectdef").find(searchDoc).projection(fieldDocument).first();
        if (defDoc == null) {
            throw new CollectionNotFoundException("_inspectdef");
        }
        JSONArray fieldsSelectedArray = JSONObject.parseObject(defDoc.toJson()).getJSONArray("fields");
        if (CollectionUtils.isNotEmpty(fieldsSelectedArray)) {

            //1、指标过滤
            Map<String, Integer> fieldsSelectMap = new HashMap<>();
            for (Object object : fieldsSelectedArray) {
                JSONObject dbObject = (JSONObject) JSON.toJSON(object);
                fieldsSelectMap.put(dbObject.getString("name"), dbObject.getInteger("selected"));
            }
            //2、根据指标获取数据结构
            for (Object object : dictionaryArray) {
                JSONObject dbObject = (JSONObject) JSON.toJSON(object);
                if (Objects.equals(fieldsSelectMap.get(dbObject.get("name")), 1)) {
                    JSONObject dictionaryObject = new JSONObject();
                    dictionaryObject.put("name", dbObject.get("name"));
                    dictionaryObject.put("desc", dbObject.get("desc"));
                    dictionaryObject.put("type", dbObject.get("type"));
                    returnFieldsArray.add(dictionaryObject);
                }
            }
            returnObject.put("fields", returnFieldsArray);

            //获取应用个性化阈值 thresholds
            //1、获取顶层阈值规则
            returnObject.put("globalThresholds", JSONObject.parseObject(defDoc.toJson()).getJSONArray("thresholds"));
            //2、应用层个性化阈值覆盖
            Document returnDoc = new Document();
            searchDoc.put("appSystemId", paramObj.getLong("appSystemId"));
            returnDoc.put("thresholds", true);
            returnDoc.put("lcd", true);
            returnDoc.put("lcu", true);
            Document defAppDoc = mongoTemplate.getDb().getCollection("_inspectdef_app").find(searchDoc).projection(returnDoc).first();
            if (defAppDoc != null) {
                JSONObject inspectDefAppJson = JSONObject.parseObject(defAppDoc.toJson());
                JSONObject lcdJson = inspectDefAppJson.getJSONObject("lcd");
                if (lcdJson != null) {
                    returnObject.put("lcd", lcdJson.getDate("$date"));
                }
                String lcu = inspectDefAppJson.getString("lcu");
                if (StringUtils.isNotEmpty(lcu)) {
                    UserVo userVo = userMapper.getUserByUuid(lcu);
                    returnObject.put("userVo", userVo);
                }
                returnObject.put("appThresholds", inspectDefAppJson.getJSONArray("thresholds"));
            }
        }
        return returnObject;
    }
}

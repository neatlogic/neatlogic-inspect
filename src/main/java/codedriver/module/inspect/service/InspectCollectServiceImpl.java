/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.inspect.service;

import codedriver.framework.cmdb.crossover.ICiCrossoverMapper;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.inspect.exception.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/10/24 10:45
 */

@Service
public class InspectCollectServiceImpl implements InspectCollectService {

    private static final Logger logger = LoggerFactory.getLogger(InspectCollectServiceImpl.class);

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private UserMapper userMapper;

    @Override
    public JSONObject getCollectionByName(String name) {
        JSONObject returnObject = new JSONObject();
        JSONArray returnFieldsArray = new JSONArray();

        //获取dictionary的数据结构（fields）
        JSONObject collectionObj = mongoTemplate.findOne(new Query(Criteria.where("name").is(name)), JSONObject.class, "_dictionary");
        if (collectionObj == null) {
            return null;
        }
        returnObject.put("label", collectionObj.getString("label"));
        returnObject.put("name", collectionObj.getString("name"));

        JSONArray dictionaryArray = collectionObj.getJSONArray("fields");

        //获取inspectDef 的指标过滤（fields）和告警规则（thresholds）
        Document doc = new Document();
        Document fieldDocument = new Document();
        doc.put("name", name);
        fieldDocument.put("fields", true);
        fieldDocument.put("thresholds", true);
        fieldDocument.put("lcd", true);
        fieldDocument.put("lcu", true);
        JSONObject inspectDefJson = JSONObject.parseObject(mongoTemplate.getDb().getCollection("_inspectdef").find(doc).projection(fieldDocument).first().toJson());
        JSONArray fieldsSelectedArray = inspectDefJson.getJSONArray("fields");

        //指标过滤Map
        Map<String, Integer> fieldsSelectMap = new HashMap<>();
        for (int i = 0; i < fieldsSelectedArray.size(); i++) {
            Object object = fieldsSelectedArray.get(i);
            try {
                JSONObject dbObject = (JSONObject) JSON.toJSON(object);
                fieldsSelectMap.put(dbObject.getString("name"), dbObject.getInteger("selected"));
            } catch (Exception ex) {
                logger.error("获取巡检“" + name + "”定义时，第" + (i + 1) + "个字典不是JSONObject，转换失败" + object.toString() + ex.getMessage(), ex);
            }
        }

        //根据指标过滤数据结构返回给前端
        for (int i = 0; i < dictionaryArray.size(); i++) {
            Object object = dictionaryArray.get(i);
            try {
                JSONObject dbObject = (JSONObject) JSON.toJSON(object);
                if (Objects.equals(fieldsSelectMap.get(dbObject.get("name")), 1)) {
                    returnFieldsArray.add(object);
                }
            } catch (Exception ex) {
                logger.error("获取巡检“" + name + "”定义时，第" + (i + 1) + "个字典不是JSONObject，转换失败:" + object.toString() + ex.getMessage(), ex);
            }
        }

        returnObject.put("fields", returnFieldsArray);
        returnObject.put("thresholds", inspectDefJson.getJSONArray("thresholds"));
        JSONObject lcdJson = inspectDefJson.getJSONObject("lcd");
        if (lcdJson != null) {
            returnObject.put("lcd", lcdJson.getDate("$date"));
        }
        String lcu = inspectDefJson.getString("lcu");
        if (StringUtils.isNotEmpty(lcu)) {
            UserVo userVo = userMapper.getUserByUuid(lcu);
            returnObject.put("userVo", userVo);
        }
        return returnObject;
    }

    @Override
    public JSONArray getAllCollection() {
        JSONArray returnArray = new JSONArray();

        //所有字典集合
        List<JSONObject> allDictionaryJsonList = mongoTemplate.find(new Query(), JSONObject.class, "_dictionary");

        //所有指标过滤（fields）和告警规则（thresholds）
        Document doc = new Document();
        Document fieldDocument = new Document();
        fieldDocument.put("fields", true);
        fieldDocument.put("name", true);
        fieldDocument.put("thresholds", true);
        FindIterable<Document> allInspectDefList = mongoTemplate.getDb().getCollection("_inspectdef").find(doc).projection(fieldDocument);
        Map<String, JSONObject> allInspectNameDefJsonMap = new HashMap<>();
        for (Document document : allInspectDefList) {
            JSONObject inspectDefJson = JSONObject.parseObject(document.toJson());
            allInspectNameDefJsonMap.put(inspectDefJson.getString("name"), inspectDefJson);
        }

        for (JSONObject dictionaryJson : allDictionaryJsonList) {
            JSONObject returnObject = new JSONObject();
            JSONArray returnFieldsArray = new JSONArray();

            returnObject.put("label", dictionaryJson.getString("label"));
            returnObject.put("name", dictionaryJson.getString("name"));

            //字典数组
            JSONArray dictionaryArray = dictionaryJson.getJSONArray("fields");

            //指标过滤Map
            Map<String, Integer> fieldsSelectMap = new HashMap<>();
            JSONObject inspectDefJson = allInspectNameDefJsonMap.get(dictionaryJson.getString("name"));
            if (inspectDefJson == null) {
                continue;
            }

            for (int i = 0; i < inspectDefJson.getJSONArray("fields").size(); i++) {
                Object object = inspectDefJson.getJSONArray("fields").get(i);
                try {
                    JSONObject dbObject = (JSONObject) JSON.toJSON(object);
                    fieldsSelectMap.put(dbObject.getString("name"), dbObject.getInteger("selected"));
                } catch (Exception ex) {
                    logger.error("获取巡检“" + dictionaryJson.getString("name") + "”定义时，第" + (i + 1) + "个字典不是JSONObject，转换失败:" + object.toString() + ex.getMessage(), ex);

                }
            }

            //根据指标过滤数据结构

            for (int i = 0; i < dictionaryArray.size(); i++) {
                Object object = dictionaryArray.get(i);
                try {
                    JSONObject dbObject = (JSONObject) JSON.toJSON(object);
                    if (Objects.equals(fieldsSelectMap.get(dbObject.get("name")), 1)) {
                        returnFieldsArray.add(object);
                    }
                } catch (Exception ex) {
                    logger.error("获取巡检“" + dictionaryJson.getString("name") + "”定义时，第" + (i + 1) + "个字典不是JSONObject，转换失败:" + object.toString() + ex.getMessage(), ex);
                }
            }

            returnObject.put("fields", returnFieldsArray);
            returnObject.put("thresholds", inspectDefJson.getJSONArray("thresholds"));
            returnArray.add(returnObject);
        }

        return returnArray;
    }

    @Override
    public List<Long> getCollectionThresholdsAppSystemIdListByResourceId(Long resourceId) {
        List<Long> returnAppSystemIdList = new ArrayList<>();
        IResourceCrossoverMapper iResourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo resourceVo = iResourceCrossoverMapper.getResourceById(resourceId);
        if (resourceVo == null) {
            throw new ResourceNotFoundException(resourceId);
        }
        ICiCrossoverMapper iCiCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo ciVo = iCiCrossoverMapper.getCiById(resourceVo.getTypeId());
        List<CiVo> parentCiList = iCiCrossoverMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht());
        if (CollectionUtils.isEmpty(parentCiList)) {
            return null;
        }

        List<String> parentCiNameList = parentCiList.stream().map(CiVo::getName).collect(Collectors.toList());
        MongoCollection<Document> collection = mongoTemplate.getCollection("_inspectdef_app");
        Document searchDoc = new Document();

        //只有OS类型的会关联多个应用实例，因此会有多个系统id
        if (parentCiNameList.contains("OS")) {
            Set<Long> resourceAppSystemIdList = iResourceCrossoverMapper.getOsResourceAppSystemIdListByOsId(resourceId);
            if (CollectionUtils.isNotEmpty(resourceAppSystemIdList)) {
                searchDoc.put("appSystemId", new Document().append("$in", resourceAppSystemIdList));
            }
            FindIterable<Document> collectionList = collection.find(searchDoc);
            if (collectionList.first() != null) {
                JSONArray defAppArray = collectionList.into(new JSONArray());
                for (Object object : defAppArray) {
                    JSONObject dbObject = (JSONObject) JSON.toJSON(object);
                    returnAppSystemIdList.add(dbObject.getLong("appSystemId"));
                }
            }
        } else {
            Long resourceAppSystemId = iResourceCrossoverMapper.getAppSystemIdByResourceId(resourceId);
            if (resourceAppSystemId != null) {
                searchDoc.put("appSystemId", resourceAppSystemId);
            }
            FindIterable<Document> collectionList = collection.find(searchDoc);
            if (collectionList.first() != null) {
                JSONObject defAppJson = JSONObject.parseObject(Objects.requireNonNull(collectionList.first()).toJson());
                returnAppSystemIdList.add(defAppJson.getLong("appSystemId"));
            }
        }
        return returnAppSystemIdList;
    }

    @Override
    public void checkThresholdsParam(JSONArray thresholds) {
        if (!org.springframework.util.CollectionUtils.isEmpty(thresholds)) {
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
                if (!thresholdTmp.containsKey("ruleUuid")) {
                    throw new InspectDefLessRuleUuidException(i);
                }

                //判断name是否重复
                String nameTmp = thresholdTmp.getString("name");
                if (nameList.contains(nameTmp)) {
                    throw new InspectDefRoleNameRepeatException(nameTmp);
                }
                nameList.add(thresholdTmp.getString("name"));
            }
        }
    }
}

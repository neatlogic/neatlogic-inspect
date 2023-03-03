/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package neatlogic.module.inspect.api.definition;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.crossover.ISyncCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.enums.sync.CollectMode;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/11/21 12:17
 */

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListInspectAppThresholdsCollectionApi extends PrivateApiComponentBase {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "获取应用巡检的阈值规则集合列表";
    }

    @Override
    public String getToken() {
        return "inspect/app/thresholds/collection/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用id"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "名称")
    })
    @Output({
            @Param(desc = "集合列表")
    })
    @Description(desc = "获取应用巡检阈值设置，需要依赖mongodb")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        ResourceSearchVo searchVo = paramObj.toJavaObject(ResourceSearchVo.class);
        List<Long> resourceTypeIdList = new ArrayList<>();
        IResourceCrossoverMapper iResourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        Set<Long> resourceTypeIdSet = iResourceCrossoverMapper.getIpObjectResourceTypeIdListByAppSystemIdAndEnvId(searchVo);
        if (CollectionUtils.isNotEmpty(resourceTypeIdSet)) {
            resourceTypeIdList.addAll(resourceTypeIdSet);
            resourceTypeIdList.addAll(iResourceCrossoverMapper.getOsResourceTypeIdListByAppSystemIdAndEnvId(searchVo));
        }

        if (CollectionUtils.isNotEmpty(resourceTypeIdList)) {
            ICiCrossoverMapper iCiCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
            List<CiVo> ciVoList = iCiCrossoverMapper.getCiByIdList(resourceTypeIdList);
            if (CollectionUtils.isNotEmpty(ciVoList)) {
                List<String> collectionNameList = new ArrayList<>();

                //获取对应的主动采集模型name searchList
                List<String> ciNameList = ciVoList.stream().map(CiVo::getName).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(ciNameList)) {
                    ISyncCrossoverMapper iSyncCrossoverMapper = CrossoverServiceFactory.getApi(ISyncCrossoverMapper.class);
                    collectionNameList = iSyncCrossoverMapper.getSyncCiCollectionNameListByCiNameListAndCollectMode(ciNameList, CollectMode.INITIATIVE.getValue());
                }

                //根据模型名称获取对应的集合信息
                if (CollectionUtils.isNotEmpty(collectionNameList)) {
                    MongoCollection<Document> collection = mongoTemplate.getCollection("_inspectdef");
                    Document searchDoc = new Document();
                    searchDoc.put("name", new Document().append("$in", collectionNameList));
                    if (StringUtils.isNotBlank(paramObj.getString("name"))) {
                        Pattern pattern = Pattern.compile("^.*" + paramObj.getString("name") + ".*$", Pattern.CASE_INSENSITIVE);
                        Document nameDoc = new Document();
                        nameDoc.put("name", pattern);
                        searchDoc.put("$and", Collections.singletonList(nameDoc));
                    }
                    FindIterable<Document> collectionList = collection.find(searchDoc);
                    return collectionList.into(new ArrayList<>());
                }
            }
        }
        return null;
    }
}

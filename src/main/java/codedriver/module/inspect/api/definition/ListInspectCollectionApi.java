/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.inspect.api.definition;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiCrossoverMapper;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.crossover.ISyncCrossoverService;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.sync.SyncCiCollectionVo;
import codedriver.framework.cmdb.enums.sync.CollectMode;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.auth.INSPECT_MODIFY;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author longrf
 * @date 2022/11/21 12:17
 */

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListInspectCollectionApi extends PrivateApiComponentBase {

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
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用id")
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
                List<String> searchList = new ArrayList<>();

                //获取对应的主动采集模型name searchList
                for (CiVo ciVo : ciVoList) {
                    ISyncCrossoverService iSyncCrossoverService = CrossoverServiceFactory.getApi(ISyncCrossoverService.class);
                    List<SyncCiCollectionVo> syncCiCollectionList = iSyncCrossoverService.searchSyncCiCollection(new SyncCiCollectionVo(ciVo.getName(), CollectMode.INITIATIVE.getValue()));
                    if (CollectionUtils.isNotEmpty(syncCiCollectionList)) {
                        for (SyncCiCollectionVo syncCiCollectionVo : syncCiCollectionList) {
                            searchList.add(syncCiCollectionVo.getCollectionName());
                        }
                    }
                }

                //根据模型名称获取对应的集合信息
                if (CollectionUtils.isNotEmpty(searchList)) {
                    MongoCollection<Document> collection = mongoTemplate.getCollection("_inspectdef");
                    Document searchDoc = new Document();
                    searchDoc.put("name", new Document().append("$in", searchList));
                    FindIterable<Document> collectionList = collection.find(searchDoc);
                    return collectionList.into(new ArrayList<>());
                }
            }
        }
        return null;
    }
}

package codedriver.module.inspect.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * @author longrf
 * @date 2022/10/24 10:45
 */

public interface InspectCollectService {

    /**
     * 获取对应的集合（名称、指标、规则）
     *
     * @param name 唯一标志
     * @return 集合（名称、指标、规则）
     */
    JSONObject getCollectionByName(String name);


    /**
     * 获取对应的集合（名称、指标、规则）
     *
     * @return 集合（名称、指标、规则）
     */
    JSONArray getAllCollection();
}

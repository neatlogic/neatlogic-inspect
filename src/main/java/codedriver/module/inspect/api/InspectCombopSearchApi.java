package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiTypeListCrossoverService;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.auth.INSPECT_MODIFY;
import codedriver.framework.inspect.dao.mapper.InspectMapper;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class InspectCombopSearchApi extends PrivateApiComponentBase {

    @Resource
    InspectMapper inspectMapper;

    @Override
    public String getName() {
        return "查询巡检组合工具列表";
    }

    @Override
    public String getToken() {
        return "inspect/combop/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciType", type = ApiParamType.STRING, desc = "ciType")})
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        List<CiVo> inspectCiList =inspectMapper.searchInspectCiCombopList();

        ICiTypeListCrossoverService ciTypeListCrossoverService = CrossoverServiceFactory.getApi(ICiTypeListCrossoverService.class);
        List<CiVo> cmdbCiList = ciTypeListCrossoverService.getCiTypeList();

        if (CollectionUtils.isEmpty(inspectCiList)) {
            return cmdbCiList;
        }
        if (CollectionUtils.isEmpty(cmdbCiList)) {
            return null;
        }
        for (CiVo ciVo : cmdbCiList) {
            List<CiVo> ciTmpList = inspectCiList.stream().filter(o -> ObjectUtils.equals(o.getId(), ciVo.getId())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(ciTmpList)) {
                continue;
            }
            ciVo.setCombopId(ciTmpList.get(0).getCombopId());
        }
        return cmdbCiList;
    }
}

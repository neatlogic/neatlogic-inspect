package codedriver.module.inspect.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiCrossoverMapper;
import codedriver.framework.cmdb.crossover.IResourceTypeTreeApiCrossoverService;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceTypeVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.auth.INSPECT_MODIFY;
import codedriver.framework.inspect.dao.mapper.InspectMapper;
import codedriver.framework.inspect.dto.InspectCiCombopVo;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
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

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字（name、label）")})
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //获取ciId和combopId的关系
        List<InspectCiCombopVo> inspectCiList = inspectMapper.searchInspectCiCombopList();

        //获取cmdb的ciType
        IResourceTypeTreeApiCrossoverService ciTypeListCrossoverService = CrossoverServiceFactory.getApi(IResourceTypeTreeApiCrossoverService.class);
        List<ResourceTypeVo> resourceTypeList = ciTypeListCrossoverService.getResourceTypeList();
        List<CiVo> ciList = new ArrayList<>();
        List<InspectCiCombopVo> ciCombopVoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(resourceTypeList)) {
            List<CiVo> ciVoList = new ArrayList<>();
            ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
            for (ResourceTypeVo type : resourceTypeList) {
                CiVo ciVo = ciCrossoverMapper.getCiByName(type.getName());
                if (ciVo == null) {
                    throw new CiNotFoundException(type.getName());
                }
                ciVoList.add(ciVo);
            }
            ciVoList.sort(Comparator.comparing(CiVo::getLft));
            for (CiVo ciVo : ciVoList) {
                List<CiVo> ciListTmp = ciCrossoverMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
                if (CollectionUtils.isNotEmpty(ciListTmp)) {
                    ciList.addAll(ciListTmp);
                }
            }
            String name = paramObj.getString("keyword");
            if (StringUtils.isNotBlank(name)) {
                ciList = ciList.stream().filter(o -> o.getLabel().contains(name)).collect(Collectors.toList());
            }
            //将List<CIVo>换成List<InspectCiCombopVo>
            for (CiVo ciVo : ciList) {
                ciCombopVoList.add(new InspectCiCombopVo(ciVo));
            }
        }

        //若关系表为空，直接返回
        if (CollectionUtils.isEmpty(inspectCiList)) {
            return ciCombopVoList;
        }
        //若cmdb的ciType列表为空，直接返回
        if (CollectionUtils.isEmpty(ciList)) {
            return Collections.emptyList();
        }
        //把关系表的combopId、combopName set到对应的InspectCiCombopVo里面
        for (InspectCiCombopVo ciCombopVo : ciCombopVoList) {
            Optional<InspectCiCombopVo> ciTmp = inspectCiList.stream().filter(o -> Objects.equals(o.getId(), ciCombopVo.getId())).findFirst();
            if (!ciTmp.isPresent()) {
                continue;
            }
            ciCombopVo.setCombopId(ciTmp.get().getCombopId());
            ciCombopVo.setCombopName(ciTmp.get().getCombopName());
        }
        return ciCombopVoList;
    }
}

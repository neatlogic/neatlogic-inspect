package neatlogic.module.inspect.api.definition;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.auth.INSPECT_MODIFY;
import neatlogic.framework.inspect.dao.mapper.InspectMapper;
import neatlogic.framework.inspect.dto.InspectCiCombopVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

import static java.util.stream.Collectors.toList;

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class InspectCombopSearchApi extends PrivateApiComponentBase {

    @Resource
    InspectMapper inspectMapper;

    @Override
    public String getName() {
        return "nmiad.inspectcombopsearchapi.getname";
    }

    @Override
    public String getToken() {
        return "inspect/combop/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword", help = "name、label")
    })
    @Description(desc = "nmiad.inspectcombopsearchapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //获取ciId和combopId的关系
        List<InspectCiCombopVo> inspectCiList = inspectMapper.searchInspectCiCombopList();

        //获取cmdb的ciType
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        List<Long> ciIdList = resourceEntityCrossoverMapper.getAllResourceTypeCiIdList();
        List<CiVo> ciList = new ArrayList<>();
        List<InspectCiCombopVo> ciCombopVoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ciIdList)) {
            ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
            List<CiVo> ciVoList = ciCrossoverMapper.getCiByIdList(ciIdList);
            ciVoList.sort(Comparator.comparing(CiVo::getLft));
            for (CiVo ciVo : ciVoList) {
                List<CiVo> ciListTmp = ciCrossoverMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
                if (CollectionUtils.isNotEmpty(ciListTmp)) {
                    ciList.addAll(ciListTmp);
                }
            }
            String keyword = paramObj.getString("keyword");
            if (StringUtils.isNotBlank(keyword)) {
                keyword = keyword.toLowerCase(Locale.ROOT);
                List<CiVo> allCiList = new ArrayList<>();
                String finalKeyword = keyword;
                List<CiVo> labelCiList = ciList.stream().filter(o -> o.getLabel().toLowerCase(Locale.ROOT).contains(finalKeyword)).collect(toList());
                List<CiVo> nameCiList = ciList.stream().filter(o -> o.getName().toLowerCase(Locale.ROOT).contains(finalKeyword)).collect(toList());
                if (CollectionUtils.isNotEmpty(labelCiList)) {
                    allCiList.addAll(labelCiList);
                }
                if (CollectionUtils.isNotEmpty(nameCiList)) {
                    allCiList.addAll(nameCiList);
                }
                ciList = allCiList.stream().distinct().collect(toList());
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

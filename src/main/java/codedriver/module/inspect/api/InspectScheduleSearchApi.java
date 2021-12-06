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
import codedriver.framework.inspect.dao.mapper.InspectScheduleMapper;
import codedriver.framework.inspect.dto.InspectScheduleVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
@AuthAction(action = INSPECT_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class InspectScheduleSearchApi extends PrivateApiComponentBase {

    @Resource
    private InspectScheduleMapper inspectScheduleMapper;

    @Override
    public String getName() {
        return "巡检定时任务列表";
    }

    @Override
    public String getToken() {
        return "inspect/schedule/search";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字（name、label）")})
    @Description(desc = "巡检定时任务列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String keyword = paramObj.getString("keyword");
        List<InspectScheduleVo> result = new ArrayList<>();
        IResourceTypeTreeApiCrossoverService ciTypeListCrossoverService = CrossoverServiceFactory.getApi(IResourceTypeTreeApiCrossoverService.class);
        List<ResourceTypeVo> resourceTypeList = ciTypeListCrossoverService.getResourceTypeList();
        if (!resourceTypeList.isEmpty()) {
            List<CiVo> ciList = new ArrayList<>();
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
            ciVoList.forEach(o -> ciList.addAll(ciCrossoverMapper.getDownwardCiListByLR(o.getLft(), o.getRht())));
            if (StringUtils.isNotEmpty(keyword)) {
                ciList.removeIf(o -> !o.getLabel().toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))
                        && !o.getName().toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT)));
            }
            List<InspectScheduleVo> inspectScheduleList = inspectScheduleMapper.getInspectScheduleList();
            for (CiVo vo : ciList) {
                Optional<InspectScheduleVo> first = inspectScheduleList.stream().filter(o -> Objects.equals(o.getCiId(), vo.getId())).findFirst();
                if (first.isPresent()) {
                    // todo 执行次数
                    InspectScheduleVo scheduleVo = first.get();
                    scheduleVo.setCiLabel(vo.getLabel());
                    scheduleVo.setCiName(vo.getName());
                    result.add(scheduleVo);
                } else {
                    result.add(new InspectScheduleVo(vo.getId(), vo.getLabel()));
                }
            }
        }
        return result;
    }
}

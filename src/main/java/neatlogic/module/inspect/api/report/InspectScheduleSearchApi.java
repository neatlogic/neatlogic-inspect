package neatlogic.module.inspect.api.report;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobInvokeVo;
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.auth.INSPECT_BASE;
import neatlogic.framework.inspect.dao.mapper.InspectScheduleMapper;
import neatlogic.framework.inspect.dto.InspectScheduleVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class InspectScheduleSearchApi extends PrivateApiComponentBase {

    @Resource
    private InspectScheduleMapper inspectScheduleMapper;

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "nmiar.inspectschedulesearchapi.getname";
    }

    @Override
    public String getToken() {
        return "inspect/schedule/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword")})
    @Output({@Param(name = "Return", explode = InspectScheduleVo[].class)})
    @Description(desc = "nmiar.inspectschedulesearchapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String keyword = paramObj.getString("keyword");
        List<InspectScheduleVo> result = new ArrayList<>();
        IResourceEntityCrossoverMapper resourceEntityCrossoverMapper = CrossoverServiceFactory.getApi(IResourceEntityCrossoverMapper.class);
        List<Long> ciIdList = resourceEntityCrossoverMapper.getAllResourceTypeCiIdList();
        if (CollectionUtils.isNotEmpty(ciIdList)) {
            List<CiVo> ciList = new ArrayList<>();
            ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
            List<CiVo> ciVoList = ciCrossoverMapper.getCiByIdList(ciIdList);
            ciVoList.sort(Comparator.comparing(CiVo::getLft));
            ciVoList.forEach(o -> ciList.addAll(ciCrossoverMapper.getDownwardCiListByLR(o.getLft(), o.getRht())));
            if (StringUtils.isNotEmpty(keyword)) {
                ciList.removeIf(o -> !o.getLabel().toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))
                        && !o.getName().toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT)));
            }
            List<InspectScheduleVo> inspectScheduleList = inspectScheduleMapper.getInspectScheduleList();
            Map<Long, Integer> execCountMap = null;
            if (!inspectScheduleList.isEmpty()) {
                List<AutoexecJobInvokeVo> execCountList = autoexecJobMapper
                        .getJobIdCountListByInvokeIdList(inspectScheduleList.stream().map(InspectScheduleVo::getId).collect(Collectors.toList()));
                execCountMap = execCountList.stream().collect(Collectors.toMap(AutoexecJobInvokeVo::getInvokeId, AutoexecJobInvokeVo::getCount));
            }

            for (CiVo vo : ciList) {
                Optional<InspectScheduleVo> first = inspectScheduleList.stream().filter(o -> Objects.equals(o.getCiId(), vo.getId())).findFirst();
                if (first.isPresent()) {
                    InspectScheduleVo scheduleVo = first.get();
                    scheduleVo.setCiLabel(vo.getLabel());
                    scheduleVo.setCiName(vo.getName());
                    if (MapUtils.isNotEmpty(execCountMap) && execCountMap.get(scheduleVo.getId()) != null) {
                        scheduleVo.setExecCount(execCountMap.get(scheduleVo.getId()));
                    }
                    result.add(scheduleVo);
                } else {
                    result.add(new InspectScheduleVo(vo.getId(), vo.getLabel(), vo.getName()));
                }
            }
        }
        return result;
    }
}

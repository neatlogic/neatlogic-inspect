/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.api.schedule;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobInvokeVo;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AppSystemVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.inspect.dao.mapper.InspectScheduleMapper;
import codedriver.framework.inspect.dto.InspectAppSystemScheduleVo;
import codedriver.framework.inspect.dto.InspectScheduleVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchInspectAppSystemScheduleApi extends PrivateApiComponentBase {

    @Resource
    private InspectScheduleMapper inspectScheduleMapper;

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getToken() {
        return "inspect/appsystem/schedule/search";
    }

    @Override
    public String getName() {
        return "查询应用定时巡检任务列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = InspectScheduleVo[].class)
    })
    @Description(desc = "查询应用定时巡检任务列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        BasePageVo searchVo = paramObj.toJavaObject(BasePageVo.class);
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        int rowNum = resourceCrossoverMapper.getAppSystemCountByKeyword(searchVo);
        if (rowNum == 0) {
            return TableResultUtil.getResult(new ArrayList<>(), searchVo);
        }
        searchVo.setRowNum(rowNum);
        List<AppSystemVo> appSystemList = resourceCrossoverMapper.getAppSystemListByKeyword(searchVo);
        if (CollectionUtils.isEmpty(appSystemList)) {
            return TableResultUtil.getResult(new ArrayList<>(), searchVo);
        }
        List<Long> appSystemIdList = appSystemList.stream().map(AppSystemVo::getId).collect(Collectors.toList());
        List<InspectAppSystemScheduleVo> inspectAppSystemScheduleList = inspectScheduleMapper.getInspectAppSystemScheduleListByAppSystemIdList(appSystemIdList);
        Map<Long, InspectAppSystemScheduleVo> inspectAppSystemScheduleMap = inspectAppSystemScheduleList.stream().collect(Collectors.toMap(e -> e.getAppSystemId(), e -> e));
        List<Long> scheduleIdList = inspectAppSystemScheduleList.stream().map(InspectAppSystemScheduleVo::getId).collect(Collectors.toList());
        Map<Long, Integer> execCountMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(scheduleIdList)) {
            List<AutoexecJobInvokeVo> execCountList = autoexecJobMapper.getJobIdCountListByInvokeIdList(scheduleIdList);
            execCountMap = execCountList.stream().collect(Collectors.toMap(AutoexecJobInvokeVo::getInvokeId, AutoexecJobInvokeVo::getCount));
        }
        List<InspectAppSystemScheduleVo> tbodyList = new ArrayList<>(appSystemList.size());
        for (AppSystemVo appSystemVo : appSystemList) {
            InspectAppSystemScheduleVo inspectAppSystemScheduleVo = inspectAppSystemScheduleMap.get(appSystemVo.getId());
            if (inspectAppSystemScheduleVo == null) {
                inspectAppSystemScheduleVo = new InspectAppSystemScheduleVo();
            } else {
                Integer execCount = execCountMap.get(inspectAppSystemScheduleVo.getId());
                if (execCount != null && execCount > 0) {
                    inspectAppSystemScheduleVo.setExecCount(execCount);
                }
            }
            inspectAppSystemScheduleVo.setAppSystemId(appSystemVo.getId());
            inspectAppSystemScheduleVo.setAppSystemName(appSystemVo.getName());
            inspectAppSystemScheduleVo.setAppSystemAbbrName(appSystemVo.getAbbrName());
            tbodyList.add(inspectAppSystemScheduleVo);
        }
        return TableResultUtil.getResult(tbodyList, searchVo);
    }
}

/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.schedule.plugin;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.crossover.ICiCrossoverMapper;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.inspect.dao.mapper.InspectMapper;
import codedriver.framework.inspect.dto.InspectAlertEverydayVo;
import codedriver.framework.inspect.dto.InspectResourceVo;
import codedriver.framework.scheduler.core.JobBase;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.module.inspect.service.InspectReportService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2022/10/28 17:42
 **/
@Component
@DisallowConcurrentExecution
public class InspectReportAlertScheduleJob extends JobBase {
    private static final String CRON_EXPRESSION = "0 0 0 * * ?";//每天凌晨0点跑
    private final Logger logger = LoggerFactory.getLogger(InspectReportAlertScheduleJob.class);
    @Resource
    InspectMapper inspectMapper;

    @Resource
    InspectReportService inspectReportService;

    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-INSPECT-REPORT-AlERT-SCHEDULE-JOB";
    }

    @Override
    public Boolean isHealthy(JobObject jobObject) {
        return true;
    }

    @Override
    public void reloadJob(JobObject jobObject) {
        String tenantUuid = jobObject.getTenantUuid();
        JobObject newJobObject = new JobObject.Builder("1", this.getGroupName(), this.getClassName(), tenantUuid).withCron(CRON_EXPRESSION).build();
        schedulerManager.loadJob(newJobObject);
    }

    @Override
    public void initJob(String tenantUuid) {
        JobObject newJobObject = new JobObject.Builder("1", this.getGroupName(), this.getClassName(), tenantUuid).withCron(CRON_EXPRESSION).build();
        schedulerManager.loadJob(newJobObject);
    }

    @Override
    public void executeInternal(JobExecutionContext context, JobObject jobObject) {
        ResourceSearchVo searchVo = new ResourceSearchVo();
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo ciVo = ciCrossoverMapper.getCiByName("IPObject");
        if (ciVo == null) {
            throw new CiNotFoundException("IPObject");
        }
        searchVo.setLft(ciVo.getLft());
        searchVo.setRht(ciVo.getRht());
        Calendar todayCalendar = Calendar.getInstance();
        todayCalendar.setTime(new Date());
        todayCalendar.set(todayCalendar.get(Calendar.YEAR), todayCalendar.get(Calendar.MONTH), todayCalendar.get(Calendar.DATE), 0, 0, 0);
        int resourceCount = inspectMapper.getInspectResourceCount(searchVo);
        if (resourceCount > 0) {
            searchVo.setRowNum(resourceCount);
            searchVo.setPageSize(20);
            for (int i = 1; i <= searchVo.getPageCount(); i++) {
                searchVo.setCurrentPage(i);
                searchVo.setStartNum(searchVo.getStartNum());
                List<Long> resourceIdList = inspectMapper.getInspectResourceIdList(searchVo);
                List<InspectResourceVo> inspectResourceVoList = inspectMapper.getInspectResourceListByIdList(resourceIdList);
                if (CollectionUtils.isNotEmpty(inspectResourceVoList)) {
                    JSONObject inspectDetail = inspectReportService.getInspectDetailByResourceIdListAndDate(inspectResourceVoList.stream().map(InspectResourceVo::getId).collect(Collectors.toList()), todayCalendar.getTime());
                    if (MapUtils.isNotEmpty(inspectDetail)) {
                        for (String key : inspectDetail.keySet()) {
                            Long resourceId = Long.valueOf(key);
                            JSONArray reportAlertArray = inspectDetail.getJSONArray(key);
                            for (int j = 0; j < reportAlertArray.size(); j++) {
                                InspectAlertEverydayVo inspectAlertEverydayVo = new InspectAlertEverydayVo(reportAlertArray.getJSONObject(j), resourceId);
                                try {
                                    inspectMapper.insertInspectAlertEveryday(inspectAlertEverydayVo);
                                }catch (Exception ex){
                                    logger.error("resourceId :"+resourceId +" :"+ex.getMessage(),ex);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

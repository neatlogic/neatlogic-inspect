/*
 * Copyright (C) 2025 TechSure Co., Ltd. All Rights Reserved.
 */
package neatlogic.module.inspect.job.callback;

import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.callback.core.AutoexecJobCallbackBase;
import neatlogic.framework.inspect.constvalue.JobSource;
import neatlogic.module.inspect.service.InspectAppJobService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Objects;

/**
 * 应用巡检子作业结束后刷新父作业状态。
 */
@Component
public class InspectAppJobCallbackHandler extends AutoexecJobCallbackBase {

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    private InspectAppJobService inspectAppJobService;

    @Override
    /** 返回回调处理器标识。 */
    public String getHandler() {
        return InspectAppJobCallbackHandler.class.getSimpleName();
    }

    @Override
    /** 仅处理应用巡检父作业下子作业的状态变化。 */
    public Boolean getIsNeedCallback(AutoexecJobVo jobVo) {
        if (jobVo == null) {
            return false;
        }
        AutoexecJobVo job = autoexecJobMapper.getJobInfo(jobVo.getId());
        if (job == null || job.getParentId() == null || Objects.equals(job.getParentId(), -1L)) {
            return false;
        }
        if (!Arrays.asList(JobSource.INSPECT_APP.getValue(), JobSource.SCHEDULE_INSPECT_APP.getValue()).contains(job.getSource())) {
            return false;
        }
        if (JobStatus.isRunningStatus(jobVo.getStatus())) {
            inspectAppJobService.refreshParentStatus(job.getParentId());
        }
        return !JobStatus.isRunningStatus(jobVo.getStatus());
    }

    @Override
    /** 子作业进入终态后刷新父作业状态。 */
    public void doService(Long invokeId, AutoexecJobVo jobVo) {
        AutoexecJobVo job = autoexecJobMapper.getJobInfo(jobVo.getId());
        if (job != null && job.getParentId() != null) {
            inspectAppJobService.refreshParentStatus(job.getParentId());
        }
    }
}

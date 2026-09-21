package neatlogic.module.inspect.service;

import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.inspect.constvalue.JobSource;
import neatlogic.module.inspect.job.callback.InspectAppJobCallbackHandler;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 使用内存 Mapper 验证应用巡检父作业状态汇总和回调时机。 */
public class InspectAppJobServiceTest {

    /** 全部子作业完成时不读取历史快照，直接将父作业更新为完成。 */
    @Test
    public void completedChildrenIgnoreSnapshotState() throws Exception {
        ParentJobStore store = new ParentJobStore(JobStatus.FAILED.getValue(),
                JobStatus.COMPLETED.getValue(), JobStatus.CHECKED.getValue());
        AutoexecJobVo parentJob = service(store).refreshParentStatus(store.parentJob.getId());
        Assert.assertEquals(JobStatus.COMPLETED.getValue(), parentJob.getStatus());
    }

    /** 任一子作业处于执行中状态时，父作业优先显示运行中。 */
    @Test
    public void runningChildTakesPriority() throws Exception {
        ParentJobStore store = new ParentJobStore(JobStatus.FAILED.getValue(),
                JobStatus.FAILED.getValue(), JobStatus.WAITING.getValue());
        AutoexecJobVo parentJob = service(store).refreshParentStatus(store.parentJob.getId());
        Assert.assertEquals(JobStatus.RUNNING.getValue(), parentJob.getStatus());
    }

    /** 失败类终态、未知状态和子作业缺失均不得被误判为完成。 */
    @Test
    public void failedUnknownAndMissingChildrenRemainFailed() throws Exception {
        for (String status : Arrays.asList(JobStatus.FAILED.getValue(), JobStatus.ABORTED.getValue(),
                JobStatus.PAUSED.getValue(), JobStatus.REVOKED.getValue(), "unknown")) {
            ParentJobStore store = new ParentJobStore(JobStatus.COMPLETED.getValue(), status);
            Assert.assertEquals(JobStatus.FAILED.getValue(),
                    service(store).refreshParentStatus(store.parentJob.getId()).getStatus());
        }

        ParentJobStore emptyStore = new ParentJobStore(JobStatus.PENDING.getValue());
        Assert.assertEquals(JobStatus.FAILED.getValue(),
                service(emptyStore).refreshParentStatus(emptyStore.parentJob.getId()).getStatus());

        ParentJobStore missingStore = new ParentJobStore(JobStatus.COMPLETED.getValue(),
                JobStatus.COMPLETED.getValue());
        missingStore.childJobIdList.add(999L);
        Assert.assertEquals(JobStatus.FAILED.getValue(),
                service(missingStore).refreshParentStatus(missingStore.parentJob.getId()).getStatus());
    }

    /** 重跑进入任一执行中状态时立即刷新父作业，终态则交给结束回调刷新。 */
    @Test
    public void callbackRefreshesRunningAndTerminalStatuses() throws Exception {
        AutoexecJobVo childJob = job(10L, JobStatus.FAILED.getValue());
        childJob.setParentId(1L);
        childJob.setSource(JobSource.INSPECT_APP.getValue());
        AutoexecJobMapper mapper = mapper((proxy, method, args) ->
                method.getName().equals("getJobInfo") ? childJob : defaultValue(method.getReturnType()));
        CountingInspectAppJobService service = new CountingInspectAppJobService();
        InspectAppJobCallbackHandler handler = new InspectAppJobCallbackHandler();
        set(handler, "autoexecJobMapper", mapper);
        set(handler, "inspectAppJobService", service);

        for (String status : Arrays.asList(JobStatus.PENDING.getValue(), JobStatus.WAITING.getValue(),
                JobStatus.RUNNING.getValue())) {
            AutoexecJobVo callbackJob = job(childJob.getId(), status);
            Assert.assertFalse(handler.getIsNeedCallback(callbackJob));
        }
        Assert.assertEquals(3, service.refreshCount);

        AutoexecJobVo completedJob = job(childJob.getId(), JobStatus.COMPLETED.getValue());
        Assert.assertTrue(handler.getIsNeedCallback(completedJob));
        handler.doService(null, completedJob);
        Assert.assertEquals(4, service.refreshCount);
    }

    /** 创建注入内存 Mapper 的服务，不配置 MongoDB 以验证状态汇总不读取快照。 */
    private InspectAppJobService service(ParentJobStore store) throws Exception {
        InspectAppJobService service = new InspectAppJobService();
        set(service, "autoexecJobMapper", mapper(store));
        return service;
    }

    /** 创建只响应状态汇总所需方法的 Mapper。 */
    private AutoexecJobMapper mapper(ParentJobStore store) {
        return mapper((proxy, method, args) -> {
            switch (method.getName()) {
                case "getJobInfo":
                    return store.parentJob;
                case "getJobIdListByParentId":
                    return store.childJobIdList;
                case "getJobListByIdList":
                    return store.childJobList;
                case "updateJobStatus":
                    store.parentJob.setStatus(((AutoexecJobVo) args[0]).getStatus());
                    return 1;
                default:
                    return defaultValue(method.getReturnType());
            }
        });
    }

    /** 创建动态 Mapper 代理。 */
    private AutoexecJobMapper mapper(java.lang.reflect.InvocationHandler handler) {
        return (AutoexecJobMapper) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{AutoexecJobMapper.class}, handler);
    }

    /** 设置测试对象的依赖字段。 */
    private void set(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /** 创建指定状态的作业。 */
    private static AutoexecJobVo job(Long id, String status) {
        AutoexecJobVo job = new AutoexecJobVo();
        job.setId(id);
        job.setStatus(status);
        return job;
    }

    /** 为动态代理返回基本类型默认值。 */
    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return null;
    }

    /** 保存父作业、直属子作业以及 Mapper 更新结果。 */
    private static class ParentJobStore {
        private final AutoexecJobVo parentJob = job(1L, JobStatus.PENDING.getValue());
        private final List<Long> childJobIdList = new ArrayList<>();
        private final List<AutoexecJobVo> childJobList = new ArrayList<>();

        private ParentJobStore(String parentStatus, String... childStatuses) {
            parentJob.setStatus(parentStatus);
            for (int i = 0; i < childStatuses.length; i++) {
                long childJobId = i + 10L;
                childJobIdList.add(childJobId);
                childJobList.add(job(childJobId, childStatuses[i]));
            }
        }
    }

    /** 只记录刷新次数的回调测试服务。 */
    private static class CountingInspectAppJobService extends InspectAppJobService {
        private int refreshCount;

        @Override
        public AutoexecJobVo refreshParentStatus(Long parentJobId) {
            refreshCount++;
            return null;
        }
    }
}

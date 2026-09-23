/*
 * Copyright (C) 2025 TechSure Co., Ltd. All Rights Reserved.
 */
package neatlogic.module.inspect.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.crossover.IAutoexecJobCrossoverService;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobInvokeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.inspect.constvalue.JobSource;
import neatlogic.framework.inspect.auth.INSPECT_EXECUTE;
import neatlogic.framework.inspect.auth.INSPECT_SCHEDULE_EXECUTE;
import neatlogic.framework.inspect.exception.InspectAppParentJobInvalidException;
import neatlogic.module.inspect.job.source.InspectAppJobRouteKey;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 应用巡检父作业及报告范围归档服务。
 */
@Service
public class InspectAppJobService {

    private static final String COLLECTION_NAME = "INSPECT_APP_JOBS";

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 初始化父作业唯一索引和应用记录查询索引。
     */
    @PostConstruct
    public void initializeIndexes() {
        mongoTemplate.indexOps(COLLECTION_NAME).ensureIndex(new Index().on("parentJobId", Sort.Direction.ASC).unique());
        mongoTemplate.indexOps(COLLECTION_NAME).ensureIndex(new Index().on("appSystemId", Sort.Direction.ASC).on("fcd", Sort.Direction.DESC));
    }

    /**
     * 创建只承担归集职责的父作业。
     *
     * @param appSystemId 应用ID
     * @param appSystemName 应用名称
     * @param source 作业来源
     * @param invokeId 来源ID
     * @return 父作业
     */
    public AutoexecJobVo createParentJob(Long appSystemId, String appSystemName, JobSource source, Long invokeId) {
        AutoexecJobVo parentJob = new AutoexecJobVo();
        parentJob.setName(appSystemName + " - " + source.getText());
        parentJob.setStatus(JobStatus.PENDING.getValue());
        parentJob.setParentId(-1L);
        parentJob.setSource(source.getValue());
        parentJob.setExecUser(UserContext.get().getUserUuid());
        parentJob.setFcu(UserContext.get().getUserUuid());
        parentJob.setInvokeId(invokeId);
        String routeId = source == JobSource.SCHEDULE_INSPECT_APP
                ? InspectAppJobRouteKey.schedule(invokeId)
                : InspectAppJobRouteKey.app(appSystemId);
        parentJob.setRouteId(routeId);
        autoexecJobMapper.insertJob(parentJob);
        IAutoexecJobSource jobSource = source;
        autoexecJobMapper.insertJobInvoke(new AutoexecJobInvokeVo(parentJob.getId(), invokeId, source.getValue(), jobSource.getType(), routeId));
        return parentJob;
    }

    /**
     * 保存本次巡检的不可变范围快照。
     *
     * @param snapshot 范围快照
     */
    public void saveSnapshot(JSONObject snapshot) {
        Document document = Document.parse(snapshot.toJSONString());
        document.put("fcd", new Date());
        document.put("fcu", UserContext.get().getUserUuid());
        mongoTemplate.getCollection(COLLECTION_NAME).insertOne(document);
    }

    /**
     * 获取父作业归档。
     *
     * @param parentJobId 父作业ID
     * @return 归档，不存在时返回null
     */
    public JSONObject getSnapshot(Long parentJobId) {
        Query query = Query.query(Criteria.where("parentJobId").is(parentJobId));
        Document document = mongoTemplate.findOne(query, Document.class, COLLECTION_NAME);
        return document == null ? null : JSONObject.parseObject(document.toJson());
    }

    /**
     * 获取应用巡检父作业及直属子作业详情。
     *
     * @param parentJobId 父作业ID
     * @return 父作业详情
     */
    public JSONObject getDetail(Long parentJobId) {
        AutoexecJobVo parentJob = validateParentJob(parentJobId);
        parentJob = refreshParentStatus(parentJobId);
        List<Long> childJobIdList = autoexecJobMapper.getJobIdListByParentId(parentJobId);
        List<AutoexecJobVo> childJobList = getChildJobList(childJobIdList);
        fillChildJobPhaseList(childJobIdList, childJobList);
        Map<Long, Integer> childAssetCountMap = getChildAssetCountMap(parentJobId);

        int runningCount = 0;
        int completedCount = 0;
        int failedCount = 0;
        JSONObject snapshot = getSnapshot(parentJobId);
        JSONArray snapshotAssetList = snapshot == null ? null : snapshot.getJSONArray("assetList");
        int assetCount = snapshotAssetList == null ? 0 : snapshotAssetList.size();
        JSONArray childJobArray = new JSONArray();
        for (AutoexecJobVo childJob : childJobList) {
            if (JobStatus.isRunningStatus(childJob.getStatus())) {
                runningCount++;
            } else if (JobStatus.isCompletedStatus(childJob.getStatus())) {
                completedCount++;
            } else {
                failedCount++;
            }
            int childAssetCount = childAssetCountMap.getOrDefault(childJob.getId(), 0);
            JSONObject childJobJson = (JSONObject) JSONObject.toJSON(childJob);
            childJobJson.put("assetCount", childAssetCount);
            childJobArray.add(childJobJson);
        }

        JSONObject summary = new JSONObject();
        summary.put("total", childJobList.size());
        summary.put("running", runningCount);
        summary.put("completed", completedCount);
        summary.put("failed", failedCount);
        summary.put("assetCount", assetCount);

        JSONObject result = new JSONObject();
        result.put("parentJob", parentJob);
        result.put("childJobList", childJobArray);
        result.put("summary", summary);
        boolean hasExecuteAuth = hasExecuteAuth(parentJob.getSource());
        boolean isExecUser = Objects.equals(parentJob.getExecUser(), UserContext.get().getUserUuid());
        boolean isChecked = Objects.equals(parentJob.getStatus(), JobStatus.CHECKED.getValue());
        result.put("isCanCheck", hasExecuteAuth && !isChecked
                && childJobList.stream().anyMatch(job -> Objects.equals(job.getStatus(), JobStatus.COMPLETED.getValue())));
        result.put("isCanExecute", hasExecuteAuth && isExecUser && !isChecked
                && !JobStatus.isRunningStatus(parentJob.getStatus()));
        result.put("isCanAbort", hasExecuteAuth && isExecUser && !isChecked);
        result.put("reportReady", !JobStatus.isRunningStatus(parentJob.getStatus()));
        return result;
    }

    /** 根据父作业来源校验对应的巡检执行权限。 */
    private boolean hasExecuteAuth(String source) {
        if (Objects.equals(source, JobSource.SCHEDULE_INSPECT_APP.getValue())) {
            return Boolean.TRUE.equals(AuthActionChecker.check(INSPECT_SCHEDULE_EXECUTE.class));
        }
        return Boolean.TRUE.equals(AuthActionChecker.check(INSPECT_EXECUTE.class));
    }

    /** 查询并补充直属子作业的模型路由及完成率。 */
    private List<AutoexecJobVo> getChildJobList(List<Long> childJobIdList) {
        if (CollectionUtils.isEmpty(childJobIdList)) {
            return List.of();
        }
        AutoexecJobVo searchVo = new AutoexecJobVo();
        searchVo.setIdList(childJobIdList);
        searchVo.setNeedPage(false);
        return getAutoexecJobService().searchJob(searchVo);
    }

    /** 获取自动化模块提供的跨模块作业服务。 */
    private IAutoexecJobCrossoverService getAutoexecJobService() {
        return CrossoverServiceFactory.getApi(IAutoexecJobCrossoverService.class);
    }

    /** 批量补充子作业步骤状态，避免详情页按卡片逐个查询。 */
    private void fillChildJobPhaseList(List<Long> childJobIdList, List<AutoexecJobVo> childJobList) {
        if (CollectionUtils.isEmpty(childJobIdList) || CollectionUtils.isEmpty(childJobList)) {
            return;
        }
        Map<Long, List<AutoexecJobPhaseVo>> jobPhaseListMap = new HashMap<>();
        List<AutoexecJobPhaseVo> phaseList = autoexecJobMapper.getJobPhaseListWithGroupByJobIdList(childJobIdList);
        if (CollectionUtils.isNotEmpty(phaseList)) {
            for (AutoexecJobPhaseVo phase : phaseList) {
                jobPhaseListMap.computeIfAbsent(phase.getJobId(), key -> new ArrayList<>()).add(phase);
            }
        }
        for (AutoexecJobVo childJob : childJobList) {
            childJob.setPhaseList(jobPhaseListMap.get(childJob.getId()));
        }
    }

    /** 根据报告快照统计每个子作业关联的资产数量。 */
    private Map<Long, Integer> getChildAssetCountMap(Long parentJobId) {
        Map<Long, Integer> childAssetCountMap = new HashMap<>();
        JSONObject snapshot = getSnapshot(parentJobId);
        JSONArray assetList = snapshot == null ? null : snapshot.getJSONArray("assetList");
        if (CollectionUtils.isNotEmpty(assetList)) {
            for (int i = 0; i < assetList.size(); i++) {
                Long childJobId = assetList.getJSONObject(i).getLong("childJobId");
                if (childJobId != null) {
                    childAssetCountMap.merge(childJobId, 1, Integer::sum);
                }
            }
        }
        return childAssetCountMap;
    }

    /** 校验父作业类型。 */
    private AutoexecJobVo validateParentJob(Long parentJobId) {
        AutoexecJobVo parentJob = autoexecJobMapper.getJobInfo(parentJobId);
        if (parentJob == null
                || !Objects.equals(parentJob.getParentId(), -1L)
                || (!Objects.equals(parentJob.getSource(), JobSource.INSPECT_APP.getValue())
                && !Objects.equals(parentJob.getSource(), JobSource.SCHEDULE_INSPECT_APP.getValue()))) {
            throw new InspectAppParentJobInvalidException(parentJobId);
        }
        return parentJob;
    }

    /**
     * 查询应用或模块的巡检父作业记录。
     *
     * @param appSystemId 应用ID
     * @param appModuleId 模块ID
     * @param currentPage 当前页
     * @param pageSize 每页数量
     * @return 分页结果
     */
    public JSONObject search(Long appSystemId, Long appModuleId, int currentPage, int pageSize) {
        Criteria criteria = Criteria.where("appSystemId").is(appSystemId);
        if (appModuleId != null) {
            criteria.and("appModuleIdList").is(appModuleId);
        }
        Query countQuery = Query.query(criteria);
        long rowNum = mongoTemplate.count(countQuery, COLLECTION_NAME);
        Query query = Query.query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "fcd"))
                .skip((long) (currentPage - 1) * pageSize)
                .limit(pageSize);
        List<Document> documentList = mongoTemplate.find(query, Document.class, COLLECTION_NAME);
        JSONArray tbodyList = new JSONArray();
        for (Document document : documentList) {
            JSONObject record = JSONObject.parseObject(document.toJson());
            record.put("fcd", document.getDate("fcd"));
            Long parentJobId = record.getLong("parentJobId");
            AutoexecJobVo parentJob = refreshParentStatus(parentJobId);
            if (parentJob != null) {
                record.put("status", parentJob.getStatus());
                record.put("statusName", JobStatus.getText(parentJob.getStatus()));
                record.put("startTime", parentJob.getStartTime());
                record.put("endTime", parentJob.getEndTime());
                record.put("execUser", parentJob.getExecUser());
            }
            tbodyList.add(record);
        }
        JSONObject result = new JSONObject();
        result.put("tbodyList", tbodyList);
        result.put("rowNum", rowNum);
        result.put("currentPage", currentPage);
        result.put("pageSize", pageSize);
        result.put("pageCount", rowNum == 0 ? 0 : (rowNum + pageSize - 1) / pageSize);
        return result;
    }

    /**
     * 根据子作业状态刷新父作业状态。
     *
     * @param parentJobId 父作业ID
     * @return 刷新后的父作业
     */
    public AutoexecJobVo refreshParentStatus(Long parentJobId) {
        AutoexecJobVo parentJob = autoexecJobMapper.getJobInfo(parentJobId);
        if (parentJob == null) {
            return null;
        }
        // 验证是父作业自身的终态，不能再由仍为 completed 的子作业覆盖。
        if (Objects.equals(parentJob.getStatus(), JobStatus.CHECKED.getValue())) {
            return parentJob;
        }
        List<Long> childJobIdList = autoexecJobMapper.getJobIdListByParentId(parentJobId);
        boolean hasRunning = false;
        boolean hasFailed = false;
        boolean allCompleted = CollectionUtils.isNotEmpty(childJobIdList);
        boolean allChecked = allCompleted;
        if (allCompleted) {
            List<AutoexecJobVo> childJobList = autoexecJobMapper.getJobListByIdList(childJobIdList);
            if (CollectionUtils.isEmpty(childJobList) || childJobList.size() != childJobIdList.size()) {
                allCompleted = false;
                allChecked = false;
                hasFailed = true;
            } else {
                for (AutoexecJobVo childJob : childJobList) {
                    String childStatus = childJob.getStatus();
                    if (JobStatus.isRunningStatus(childStatus)) {
                        hasRunning = true;
                        allCompleted = false;
                        allChecked = false;
                    } else if (JobStatus.isFailedStatus(childStatus)) {
                        hasFailed = true;
                        allCompleted = false;
                        allChecked = false;
                    } else if (!JobStatus.isCompletedStatus(childStatus)) {
                        // 未知状态不能被误判为完成，按失败处理以便及时暴露数据异常。
                        hasFailed = true;
                        allCompleted = false;
                        allChecked = false;
                    } else if (!Objects.equals(childStatus, JobStatus.CHECKED.getValue())) {
                        allChecked = false;
                    }
                }
            }
        }
        String status = hasRunning ? JobStatus.RUNNING.getValue()
                : hasFailed || !allCompleted ? JobStatus.FAILED.getValue()
                : allChecked ? JobStatus.CHECKED.getValue() : JobStatus.COMPLETED.getValue();
        if (!Objects.equals(status, parentJob.getStatus())) {
            AutoexecJobVo updateJob = new AutoexecJobVo(parentJobId, status);
            autoexecJobMapper.updateJobStatus(updateJob);
            parentJob = autoexecJobMapper.getJobInfo(parentJobId);
        }
        return parentJob;
    }

    /**
     * 判断父作业是否已结束并允许下载报告。
     *
     * @param parentJobId 父作业ID
     * @return 是否允许下载
     */
    public boolean isReportReady(Long parentJobId) {
        AutoexecJobVo parentJob = refreshParentStatus(parentJobId);
        return parentJob != null && !JobStatus.isRunningStatus(parentJob.getStatus());
    }
}

package neatlogic.module.inspect.dto;

import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.util.SnowflakeUtil;

import java.util.Date;

public class InspectConfigCompareTaskVo extends BasePageVo {
    private Long id;
    private String compareType;
    private Long baselineVersionId;
    private Long sourceSnapshotId;
    private Long targetSnapshotId;
    private Long appSystemId;
    private Long appModuleId;
    private Long envId;
    private String status;
    private String compareResult;
    private String riskLevel;
    private Integer isBlocked;
    private Integer totalCount;
    private Integer diffCount;
    private Integer highCount;
    private Integer mediumCount;
    private Integer lowCount;
    private String summary;
    private String aiSummary;
    private Date fcd;
    private String fcu;
    private Date lcd;
    private String lcu;

    public Long getId() {
        if (id == null) {
            id = SnowflakeUtil.uniqueLong();
        }
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCompareType() {
        return compareType;
    }

    public void setCompareType(String compareType) {
        this.compareType = compareType;
    }

    public Long getBaselineVersionId() {
        return baselineVersionId;
    }

    public void setBaselineVersionId(Long baselineVersionId) {
        this.baselineVersionId = baselineVersionId;
    }

    public Long getSourceSnapshotId() {
        return sourceSnapshotId;
    }

    public void setSourceSnapshotId(Long sourceSnapshotId) {
        this.sourceSnapshotId = sourceSnapshotId;
    }

    public Long getTargetSnapshotId() {
        return targetSnapshotId;
    }

    public void setTargetSnapshotId(Long targetSnapshotId) {
        this.targetSnapshotId = targetSnapshotId;
    }

    public Long getAppSystemId() {
        return appSystemId;
    }

    public void setAppSystemId(Long appSystemId) {
        this.appSystemId = appSystemId;
    }

    public Long getAppModuleId() {
        return appModuleId;
    }

    public void setAppModuleId(Long appModuleId) {
        this.appModuleId = appModuleId;
    }

    public Long getEnvId() {
        return envId;
    }

    public void setEnvId(Long envId) {
        this.envId = envId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCompareResult() {
        return compareResult;
    }

    public void setCompareResult(String compareResult) {
        this.compareResult = compareResult;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Integer getIsBlocked() {
        return isBlocked;
    }

    public void setIsBlocked(Integer isBlocked) {
        this.isBlocked = isBlocked;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getDiffCount() {
        return diffCount;
    }

    public void setDiffCount(Integer diffCount) {
        this.diffCount = diffCount;
    }

    public Integer getHighCount() {
        return highCount;
    }

    public void setHighCount(Integer highCount) {
        this.highCount = highCount;
    }

    public Integer getMediumCount() {
        return mediumCount;
    }

    public void setMediumCount(Integer mediumCount) {
        this.mediumCount = mediumCount;
    }

    public Integer getLowCount() {
        return lowCount;
    }

    public void setLowCount(Integer lowCount) {
        this.lowCount = lowCount;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public Date getFcd() {
        return fcd;
    }

    public void setFcd(Date fcd) {
        this.fcd = fcd;
    }

    public String getFcu() {
        return fcu;
    }

    public void setFcu(String fcu) {
        this.fcu = fcu;
    }

    public Date getLcd() {
        return lcd;
    }

    public void setLcd(Date lcd) {
        this.lcd = lcd;
    }

    public String getLcu() {
        return lcu;
    }

    public void setLcu(String lcu) {
        this.lcu = lcu;
    }
}

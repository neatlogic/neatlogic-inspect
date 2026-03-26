package neatlogic.module.inspect.dto;

import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.util.SnowflakeUtil;

import java.util.Date;

public class InspectConfigBaselineVersionVo extends BasePageVo {
    private Long id;
    private Long baselineId;
    private String version;
    private String status;
    private Integer isFrozen;
    private String sourceType;
    private Long sourceSnapshotId;
    private Integer fieldCount;
    private String baselineData;
    private String aiCandidateData;
    private String changeSummary;
    private String changeLog;
    private Date approvedTime;
    private Date activatedTime;
    private Integer isCurrentActive;
    private String approvalStatus;
    private String approver;
    private String approvalComment;
    private UserVo approverVo;
    private Date actionTime;
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

    public Long getBaselineId() {
        return baselineId;
    }

    public void setBaselineId(Long baselineId) {
        this.baselineId = baselineId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getIsFrozen() {
        return isFrozen;
    }

    public void setIsFrozen(Integer isFrozen) {
        this.isFrozen = isFrozen;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceSnapshotId() {
        return sourceSnapshotId;
    }

    public void setSourceSnapshotId(Long sourceSnapshotId) {
        this.sourceSnapshotId = sourceSnapshotId;
    }

    public Integer getFieldCount() {
        return fieldCount;
    }

    public void setFieldCount(Integer fieldCount) {
        this.fieldCount = fieldCount;
    }

    public String getBaselineData() {
        return baselineData;
    }

    public void setBaselineData(String baselineData) {
        this.baselineData = baselineData;
    }

    public String getAiCandidateData() {
        return aiCandidateData;
    }

    public void setAiCandidateData(String aiCandidateData) {
        this.aiCandidateData = aiCandidateData;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    public String getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(String changeLog) {
        this.changeLog = changeLog;
    }

    public Date getApprovedTime() {
        return approvedTime;
    }

    public void setApprovedTime(Date approvedTime) {
        this.approvedTime = approvedTime;
    }

    public Date getActivatedTime() {
        return activatedTime;
    }

    public void setActivatedTime(Date activatedTime) {
        this.activatedTime = activatedTime;
    }

    public Integer getIsCurrentActive() {
        return isCurrentActive;
    }

    public void setIsCurrentActive(Integer isCurrentActive) {
        this.isCurrentActive = isCurrentActive;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approver) {
        this.approver = approver;
    }

    public String getApprovalComment() {
        return approvalComment;
    }

    public void setApprovalComment(String approvalComment) {
        this.approvalComment = approvalComment;
    }

    public UserVo getApproverVo() {
        return approverVo;
    }

    public void setApproverVo(UserVo approverVo) {
        this.approverVo = approverVo;
    }

    public Date getActionTime() {
        return actionTime;
    }

    public void setActionTime(Date actionTime) {
        this.actionTime = actionTime;
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

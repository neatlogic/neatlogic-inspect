package neatlogic.module.inspect.dto;

import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.util.SnowflakeUtil;

import java.util.Date;

public class InspectConfigBaselineVo extends BasePageVo {
    private Long id;
    private Long appSystemId;
    private Long appModuleId;
    private Long envId;
    private Long typeId;
    private String schemaName;
    private String scopeHash;
    private String name;
    private String description;
    private Long currentVersionId;
    private String currentVersion;
    private String currentStatus;
    private Integer currentFieldCount;
    private Date currentActivatedTime;
    private String currentPublisher;
    private UserVo currentPublisherVo;
    private Long currentSourceSnapshotId;
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

    public Long getTypeId() {
        return typeId;
    }

    public void setTypeId(Long typeId) {
        this.typeId = typeId;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getScopeHash() {
        return scopeHash;
    }

    public void setScopeHash(String scopeHash) {
        this.scopeHash = scopeHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCurrentVersionId() {
        return currentVersionId;
    }

    public void setCurrentVersionId(Long currentVersionId) {
        this.currentVersionId = currentVersionId;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public Integer getCurrentFieldCount() {
        return currentFieldCount;
    }

    public void setCurrentFieldCount(Integer currentFieldCount) {
        this.currentFieldCount = currentFieldCount;
    }

    public Date getCurrentActivatedTime() {
        return currentActivatedTime;
    }

    public void setCurrentActivatedTime(Date currentActivatedTime) {
        this.currentActivatedTime = currentActivatedTime;
    }

    public String getCurrentPublisher() {
        return currentPublisher;
    }

    public void setCurrentPublisher(String currentPublisher) {
        this.currentPublisher = currentPublisher;
    }

    public UserVo getCurrentPublisherVo() {
        return currentPublisherVo;
    }

    public void setCurrentPublisherVo(UserVo currentPublisherVo) {
        this.currentPublisherVo = currentPublisherVo;
    }

    public Long getCurrentSourceSnapshotId() {
        return currentSourceSnapshotId;
    }

    public void setCurrentSourceSnapshotId(Long currentSourceSnapshotId) {
        this.currentSourceSnapshotId = currentSourceSnapshotId;
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

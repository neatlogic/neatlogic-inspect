package neatlogic.module.inspect.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import neatlogic.framework.ai.dao.mapper.AiModelMapper;
import neatlogic.framework.ai.dto.model.AiModelVo;
import neatlogic.framework.ai.enums.AiModelType;
import neatlogic.framework.ai.model.core.AiModelFactory;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.util.Md5Util;
import neatlogic.module.inspect.dao.mapper.InspectConfigCompareMapper;
import neatlogic.module.inspect.dto.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class InspectConfigCompareService {
    private static final Logger logger = LoggerFactory.getLogger(InspectConfigCompareService.class);

    private static final String APPROVAL_STAGE = "approval";
    private static final String APPROVAL_STATUS_PENDING = "pending";
    private static final String APPROVAL_STATUS_APPROVED = "approved";
    private static final String APPROVAL_STATUS_REJECTED = "rejected";
    private static final String AI_CALL_MODE_CHAT = "chat";
    private static final String AI_CALL_MODE_STREAM = "stream";
    private static final int AI_FIELD_LIMIT = 80;
    private static final String DEFAULT_AI_CANDIDATE_SYSTEM_PROMPT = "你是巡检配置基线候选筛选器。"
            + "你只能从输入字段中选择已有字段，不得编造字段，不得修改字段值。"
            + "保留以下类型："
            + "硬件层(CPU型号、内存容量、磁盘阵列、BIOS版本、RAID设置、网卡驱动等)；"
            + "内核层(内核版本、加载模块、sysctl参数(如vm.swappiness、net.core.somaxconn))；"
            + "操作系统层(系统版本、补丁级别、用户权限、防火墙规则、SELinux状态、计划任务等)；"
            + "应用层(中间件配置（Nginx/Apache）、数据库连接参数、JVM启动参数、环境变量、日志路径等)；"
            + "输入只包含字段元信息，请仅根据name、label、type、layer判断。"
            + "输出必须是JSON对象，格式为："
            + "{\"included\":[{\"path\":\"字段name\",\"reason\":\"纳入原因\",\"compareMode\":\"exact|set|object_by_key\",\"confidence\":0.95}],"
            + "\"excluded\":[{\"path\":\"字段name\",\"reason\":\"排除原因\"}],"
            + "\"summary\":\"一句话总结\"}。"
            + "如果不确定，也不要编造，宁可放到excluded。";

    private static final Set<String> IGNORE_FIELD_SET = new HashSet<>(Arrays.asList(
            "HOSTNAME", "MGMT_IP", "MGMT_PORT", "STATE", "AVAILABILITY", "RESPONSE_TIME", "ERROR_MESSAGE",
            "RESOURCE_ID", "OS_ID", "MACHINE_ID", "PK", "BIZ_IP", "BOARD_SERIAL", "PRODUCT_UUID",
            "CPU_USAGE", "CPU_USAGE_PERCORE", "CPU_LOAD_AVG_1", "CPU_LOAD_AVG_5", "CPU_LOAD_AVG_15", "IOWAIT_PCT",
            "MEM_AVAILABLE", "MEM_FREE", "MEM_USAGE", "MEM_BUFFERS", "MEM_CACHED", "SWAP_FREE", "UPTIME",
            "NTP_OFFSET_SECS", "DEFUNC_PROCESSES_COUNT", "TOP_CPU_RPOCESSES", "TOP_MEM_PROCESSES",
            "LISTEN_PORTS", "SERVICE_PORTS", "IP_ADDRS", "IPV4_ADDRS", "IPV6_ADDRS"
    ));

    private static final Set<String> HARDWARE_FIELD_SET = new HashSet<>(Arrays.asList(
            "IS_VIRTUAL", "SYS_VENDOR", "PRODUCT_NAME", "CPU_MODEL", "CPU_ARCH", "CPU_CORES", "CPU_LOGIC_CORES",
            "CPU_FREQUENCY", "CPU_VERSION", "CPU_FIRMWARE_VERSION", "CPU_MICROCODE", "CPU_BITS", "MEM_TOTAL",
            "SWAP_TOTAL", "DISKS", "ETH_INTERFACES", "NIC_BOND", "NIC_TEAM", "HBA_INTERFACES", "NFS_INFO"
    ));

    private static final Set<String> KERNEL_FIELD_SET = new HashSet<>(Collections.singletonList("KERNEL_VERSION"));

    private static final Set<String> OS_FIELD_SET = new HashSet<>(Arrays.asList(
            "OS_TYPE", "MAJOR_VERSION", "VERSION", "USERS", "MOUNT_POINTS", "DNS_SERVERS", "NTP_ENABLE",
            "NTP_SERVERS", "SSH_VERSION", "OPENSSL_VERSION", "DEFAULT_GATEWAY", "PATCHES_APPLIED",
            "FIREWALL_ENABLE", "SELINUX_STATUS", "IP_RULES", "NETWORKMANAGER_ENABLE", "MAX_OPEN_FILES",
            "MAX_USER_PROCESS_COUNT", "AUTO_RESTART", "NFS_MOUNTED"
    ));

    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private InspectConfigCompareMapper inspectConfigCompareMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private AiModelMapper aiModelMapper;

    public List<InspectConfigBaselineVo> searchBaselineList(Long appSystemId, Long appModuleId, Long envId, String schemaName) {
        InspectConfigBaselineVo searchVo = new InspectConfigBaselineVo();
        searchVo.setAppSystemId(appSystemId);
        searchVo.setAppModuleId(appModuleId);
        searchVo.setEnvId(envId);
        searchVo.setSchemaName(schemaName);
        List<InspectConfigBaselineVo> baselineList = inspectConfigCompareMapper.getBaselineList(searchVo);
        fillBaselineUserVo(baselineList);
        return baselineList;
    }

    public JSONObject getAiSettingData(String schemaName) {
        if (StringUtils.isBlank(schemaName)) {
            throw new ParamNotExistsException("schemaName");
        }
        InspectConfigAiSettingVo settingVo = buildAiSettingScope(schemaName);
        InspectConfigAiSettingVo currentSetting = inspectConfigCompareMapper.getAiSettingByScope(settingVo);
        String defaultPrompt = getDefaultAiCandidateSystemPrompt();
        AiModelVo currentModel = currentSetting != null && currentSetting.getModelId() != null ? aiModelMapper.getAiModelById(currentSetting.getModelId()) : null;
        AiModelVo query = new AiModelVo();
        query.setModelType(AiModelType.CHAT.getValue());
        List<AiModelVo> modelList = aiModelMapper.searchAiModel(query);
        JSONArray modelOptionList = new JSONArray();
        if (CollectionUtils.isNotEmpty(modelList)) {
            for (AiModelVo modelVo : modelList) {
                if (modelVo == null || modelVo.getId() == null) {
                    continue;
                }
                JSONObject option = new JSONObject(true);
                option.put("value", modelVo.getId());
                option.put("text", StringUtils.defaultIfBlank(modelVo.getName(), modelVo.getModelName()));
                option.put("modelName", modelVo.getModelName());
                option.put("description", modelVo.getDescription());
                modelOptionList.add(option);
            }
        }
        JSONObject result = new JSONObject(true);
        result.put("setting", currentSetting);
        result.put("model", currentModel);
        result.put("modelList", modelOptionList);
        result.put("defaultPrompt", defaultPrompt);
        result.put("effectivePrompt", currentSetting != null && StringUtils.isNotBlank(currentSetting.getPrompt()) ? currentSetting.getPrompt() : defaultPrompt);
        return result;
    }

    @Transactional
    public InspectConfigAiSettingVo saveAiSetting(String schemaName, Long modelId, String prompt) {
        if (StringUtils.isBlank(schemaName)) {
            throw new ParamNotExistsException("schemaName");
        }
        if (modelId == null) {
            throw new ParamNotExistsException("modelId");
        }
        AiModelVo modelVo = aiModelMapper.getAiModelById(modelId);
        if (modelVo == null || !Objects.equals(modelVo.getModelType(), AiModelType.CHAT.getValue())) {
            throw new ParamNotExistsException("model");
        }
        String normalizedPrompt = normalizeAiSettingPrompt(prompt);
        InspectConfigAiSettingVo scopeVo = buildAiSettingScope(schemaName);
        InspectConfigAiSettingVo currentSetting = inspectConfigCompareMapper.getAiSettingByScope(scopeVo);
        String userUuid = UserContext.get().getUserUuid(true);
        if (currentSetting == null) {
            scopeVo.setModelId(modelId);
            scopeVo.setPrompt(normalizedPrompt);
            scopeVo.setFcu(userUuid);
            scopeVo.setLcu(userUuid);
            inspectConfigCompareMapper.insertAiSetting(scopeVo);
            return scopeVo;
        }
        currentSetting.setModelId(modelId);
        currentSetting.setPrompt(normalizedPrompt);
        currentSetting.setLcu(userUuid);
        inspectConfigCompareMapper.updateAiSetting(currentSetting);
        return currentSetting;
    }

    public InspectConfigBaselineVersionVo getBaselineVersionById(Long id) {
        InspectConfigBaselineVersionVo versionVo = inspectConfigCompareMapper.getBaselineVersionById(id);
        fillVersionUserVo(versionVo);
        return versionVo;
    }

    public List<InspectConfigBaselineVersionVo> getBaselineVersionListByBaselineId(Long baselineId) {
        if (baselineId == null) {
            throw new ParamNotExistsException("baselineId");
        }
        List<InspectConfigBaselineVersionVo> versionList = inspectConfigCompareMapper.getBaselineVersionListByBaselineId(baselineId);
        fillVersionUserVo(versionList);
        return versionList;
    }

    @Transactional
    public void deleteBaseline(Long baselineId) {
        if (baselineId == null) {
            throw new ParamNotExistsException("id");
        }
        InspectConfigBaselineVo baselineVo = inspectConfigCompareMapper.getBaselineById(baselineId);
        if (baselineVo == null) {
            throw new ParamNotExistsException("baseline");
        }
        inspectConfigCompareMapper.deleteCompareDetailByBaselineId(baselineId);
        inspectConfigCompareMapper.deleteCompareTaskByBaselineId(baselineId);
        inspectConfigCompareMapper.deleteBaselineRuleByBaselineId(baselineId);
        inspectConfigCompareMapper.deleteBaselineVersionByBaselineId(baselineId);
        inspectConfigCompareMapper.deleteBaselineById(baselineId);
    }

    @Transactional
    public boolean deleteBaselineVersion(Long versionId) {
        if (versionId == null) {
            throw new ParamNotExistsException("id");
        }
        InspectConfigBaselineVersionVo versionVo = getBaselineVersionById(versionId);
        if (versionVo == null) {
            throw new ParamNotExistsException("version");
        }
        if (Objects.equals(versionVo.getIsCurrentActive(), 1) || Objects.equals(versionVo.getStatus(), "active")) {
            throw new ApiRuntimeException("当前生效版本不允许直接删除，请先切换到其他版本");
        }
        Long baselineId = versionVo.getBaselineId();
        inspectConfigCompareMapper.deleteCompareDetailByBaselineVersionId(versionId);
        inspectConfigCompareMapper.deleteCompareTaskByBaselineVersionId(versionId);
        inspectConfigCompareMapper.deleteBaselineVersionById(versionId);
        Integer remainCount = inspectConfigCompareMapper.getBaselineVersionCountByBaselineId(baselineId);
        if (remainCount == null || remainCount == 0) {
            inspectConfigCompareMapper.deleteBaselineById(baselineId);
            return true;
        }
        return false;
    }

    public InspectConfigBaselineVersionVo saveDraftBaselineVersion(Long versionId, String baselineData) {
        if (versionId == null) {
            throw new ParamNotExistsException("id");
        }
        if (StringUtils.isBlank(baselineData)) {
            throw new ParamNotExistsException("baselineData");
        }
        InspectConfigBaselineVersionVo versionVo = inspectConfigCompareMapper.getBaselineVersionById(versionId);
        if (versionVo == null) {
            throw new ParamNotExistsException("version");
        }
        if (!Objects.equals("draft", versionVo.getStatus()) && !Objects.equals("rejected", versionVo.getStatus())) {
            throw new ApiRuntimeException("只有草稿或已驳回版本允许编辑");
        }
        JSONObject normalizedBaseline = normalizeBaselineDraft(JSONObject.parseObject(baselineData));
        JSONObject summary = buildSummary(normalizedBaseline);
        String userUuid = UserContext.get().getUserUuid(true);
        versionVo.setFieldCount(getFieldCount(normalizedBaseline.toJSONString()));
        versionVo.setBaselineData(normalizedBaseline.toJSONString());
        versionVo.setChangeSummary("草稿已手工编辑");
        versionVo.setChangeLog(summary.toJSONString());
        versionVo.setLcu(userUuid);
        inspectConfigCompareMapper.updateBaselineVersionContent(versionVo);
        return getBaselineVersionById(versionId);
    }

    @Transactional
    public InspectConfigBaselineVersionVo submitBaselineVersionForApproval(Long versionId) {
        if (versionId == null) {
            throw new ParamNotExistsException("id");
        }
        InspectConfigBaselineVersionVo versionVo = inspectConfigCompareMapper.getBaselineVersionById(versionId);
        if (versionVo == null) {
            throw new ParamNotExistsException("version");
        }
        if (!Objects.equals("draft", versionVo.getStatus()) && !Objects.equals("rejected", versionVo.getStatus())) {
            throw new ApiRuntimeException("只有草稿或已驳回版本允许提交审核");
        }
        String userUuid = UserContext.get().getUserUuid(true);
        versionVo.setStatus("pending_approval");
        versionVo.setApprovedTime(null);
        versionVo.setActivatedTime(null);
        versionVo.setApprovalStatus(APPROVAL_STATUS_PENDING);
        versionVo.setApprover(null);
        versionVo.setApprovalComment(null);
        versionVo.setLcu(userUuid);
        inspectConfigCompareMapper.updateBaselineVersionStatusById(versionVo);
        return getBaselineVersionById(versionId);
    }

    @Transactional
    public InspectConfigBaselineVersionVo auditBaselineVersion(Long versionId, String stage, String status, String comment) {
        if (versionId == null) {
            throw new ParamNotExistsException("id");
        }
        if (StringUtils.isNotBlank(stage) && !Objects.equals(APPROVAL_STAGE, stage)) {
            throw new ParamNotExistsException("stage");
        }
        if (!Objects.equals(APPROVAL_STATUS_APPROVED, status) && !Objects.equals(APPROVAL_STATUS_REJECTED, status)) {
            throw new ParamNotExistsException("status");
        }
        InspectConfigBaselineVersionVo versionVo = inspectConfigCompareMapper.getBaselineVersionById(versionId);
        if (versionVo == null) {
            throw new ParamNotExistsException("version");
        }
        if (!Objects.equals("pending_approval", versionVo.getStatus())) {
            throw new ApiRuntimeException("当前版本不在待审批状态");
        }
        String userUuid = UserContext.get().getUserUuid(true);
        Date now = new Date();
        if (Objects.equals(APPROVAL_STATUS_REJECTED, status)) {
            versionVo.setStatus("rejected");
            versionVo.setApprovedTime(null);
            versionVo.setActivatedTime(null);
            versionVo.setApprovalStatus(APPROVAL_STATUS_REJECTED);
            versionVo.setApprover(userUuid);
            versionVo.setApprovalComment(comment);
            versionVo.setLcu(userUuid);
            inspectConfigCompareMapper.updateBaselineVersionStatusById(versionVo);
            return getBaselineVersionById(versionId);
        }
        versionVo.setStatus("approved");
        versionVo.setApprovedTime(now);
        versionVo.setActivatedTime(null);
        versionVo.setApprovalStatus(APPROVAL_STATUS_APPROVED);
        versionVo.setApprover(userUuid);
        versionVo.setApprovalComment(comment);
        versionVo.setLcu(userUuid);
        inspectConfigCompareMapper.updateBaselineVersionStatusById(versionVo);
        return getBaselineVersionById(versionId);
    }

    public InspectConfigBaselineVersionVo publishBaselineVersion(Long versionId) {
        if (versionId == null) {
            throw new ParamNotExistsException("id");
        }
        InspectConfigBaselineVersionVo versionVo = inspectConfigCompareMapper.getBaselineVersionById(versionId);
        if (versionVo == null) {
            throw new ParamNotExistsException("version");
        }
        if (!Objects.equals("approved", versionVo.getStatus()) && !Objects.equals("active", versionVo.getStatus())) {
            throw new ApiRuntimeException("只有已审批或已发布版本允许发布");
        }
        return activateBaselineVersion(versionVo);
    }

    @Transactional
    public InspectConfigBaselineVersionVo rollbackBaselineVersion(Long versionId) {
        if (versionId == null) {
            throw new ParamNotExistsException("id");
        }
        InspectConfigBaselineVersionVo versionVo = inspectConfigCompareMapper.getBaselineVersionById(versionId);
        if (versionVo == null) {
            throw new ParamNotExistsException("version");
        }
        if (!Objects.equals("approved", versionVo.getStatus()) && !Objects.equals("active", versionVo.getStatus())) {
            throw new ApiRuntimeException("只有已审批或已发布版本允许回退");
        }
        return activateBaselineVersion(versionVo);
    }

    public InspectConfigSnapshotVo getSnapshotById(Long id) {
        return inspectConfigCompareMapper.getSnapshotById(id);
    }

    public InspectConfigSnapshotVo generateSnapshot(Long appSystemId, Long appModuleId, Long envId, Long typeId, Long resourceId, String schemaName) {
        if (resourceId == null) {
            throw new ParamNotExistsException("resourceId");
        }
        if (appSystemId == null) {
            throw new ParamNotExistsException("appSystemId");
        }
        if (StringUtils.isBlank(schemaName)) {
            throw new ParamNotExistsException("schemaName");
        }
        JSONObject collectJson = getLatestCollectJson(resourceId, schemaName);
        if (MapUtils.isEmpty(collectJson)) {
            throw new ParamNotExistsException("当前节点暂无可用采集快照，请先执行巡检或采集");
        }
        JSONObject normalizedData = normalizeReport(schemaName, collectJson);
        JSONObject summary = buildSummary(normalizedData);
        InspectConfigSnapshotVo snapshotVo = new InspectConfigSnapshotVo();
        snapshotVo.setAppSystemId(appSystemId);
        snapshotVo.setAppModuleId(appModuleId);
        snapshotVo.setEnvId(envId);
        snapshotVo.setTypeId(typeId);
        snapshotVo.setResourceId(resourceId);
        snapshotVo.setSchemaName(schemaName);
        snapshotVo.setSchemaVersion("1.0");
        snapshotVo.setSource("collect");
        snapshotVo.setStatus("succeed");
        snapshotVo.setCollectTime(extractCollectTime(collectJson));
        snapshotVo.setRawData(collectJson.toJSONString());
        snapshotVo.setNormalizedData(normalizedData.toJSONString());
        snapshotVo.setSummary(summary.toJSONString());
        String userUuid = UserContext.get().getUserUuid(true);
        snapshotVo.setFcu(userUuid);
        snapshotVo.setLcu(userUuid);
        inspectConfigCompareMapper.insertSnapshot(snapshotVo);
        return snapshotVo;
    }

    public InspectConfigBaselineVersionVo promoteResourceToBaseline(Long appSystemId, Long appModuleId, Long envId, Long typeId, Long resourceId, String schemaName, String baselineName, String description) {
        InspectConfigSnapshotVo snapshotVo = generateSnapshot(appSystemId, appModuleId, envId, typeId, resourceId, schemaName);
        String scopeHash = buildScopeHash(appSystemId, appModuleId, envId, typeId, schemaName);
        InspectConfigBaselineVo baselineVo = inspectConfigCompareMapper.getBaselineByScopeHash(scopeHash);
        String userUuid = UserContext.get().getUserUuid(true);
        if (baselineVo == null) {
            baselineVo = new InspectConfigBaselineVo();
            baselineVo.setAppSystemId(appSystemId);
            baselineVo.setAppModuleId(appModuleId);
            baselineVo.setEnvId(envId);
            baselineVo.setTypeId(typeId);
            baselineVo.setSchemaName(schemaName);
            baselineVo.setScopeHash(scopeHash);
            baselineVo.setName(StringUtils.defaultIfBlank(baselineName, schemaName + "基线"));
            baselineVo.setDescription(description);
            baselineVo.setFcu(userUuid);
            baselineVo.setLcu(userUuid);
            inspectConfigCompareMapper.insertBaseline(baselineVo);
        }
        JSONObject draftCandidate = buildCandidateDraft(snapshotVo);
        JSONObject baselineDraft = buildBaselineDraft(snapshotVo, draftCandidate);
        InspectConfigBaselineVersionVo versionVo = new InspectConfigBaselineVersionVo();
        versionVo.setBaselineId(baselineVo.getId());
        versionVo.setVersion("v" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()));
        versionVo.setStatus("draft");
        versionVo.setIsFrozen(0);
        versionVo.setSourceType("candidate");
        versionVo.setSourceSnapshotId(snapshotVo.getId());
        versionVo.setFieldCount(getFieldCount(baselineDraft.toJSONString()));
        versionVo.setBaselineData(baselineDraft.toJSONString());
        versionVo.setAiCandidateData(draftCandidate.toJSONString());
        versionVo.setChangeSummary(buildBaselineDraftChangeSummary(resourceId, draftCandidate));
        versionVo.setChangeLog(buildSummary(baselineDraft).toJSONString());
        versionVo.setFcu(userUuid);
        versionVo.setLcu(userUuid);
        inspectConfigCompareMapper.insertBaselineVersion(versionVo);
        baselineVo.setName(StringUtils.defaultIfBlank(baselineName, baselineVo.getName()));
        baselineVo.setDescription(description);
        baselineVo.setLcu(userUuid);
        inspectConfigCompareMapper.updateBaselineCurrentVersion(baselineVo);
        return getBaselineVersionById(versionVo.getId());
    }

    public JSONObject compareWithBaseline(Long appSystemId, Long appModuleId, Long envId, Long typeId, Long resourceId, String schemaName) {
        String scopeHash = buildScopeHash(appSystemId, appModuleId, envId, typeId, schemaName);
        InspectConfigBaselineVo baselineVo = inspectConfigCompareMapper.getBaselineByScopeHash(scopeHash);
        if (baselineVo == null || baselineVo.getCurrentVersionId() == null) {
            throw new ParamNotExistsException("当前作用域尚未建立配置基线");
        }
        InspectConfigBaselineVersionVo versionVo = inspectConfigCompareMapper.getBaselineVersionById(baselineVo.getCurrentVersionId());
        InspectConfigSnapshotVo snapshotVo = generateSnapshot(appSystemId, appModuleId, envId, typeId, resourceId, schemaName);
        return doCompare("baseline", appSystemId, appModuleId, envId, snapshotVo, versionVo, null);
    }

    private InspectConfigBaselineVersionVo activateBaselineVersion(InspectConfigBaselineVersionVo versionVo) {
        if (Objects.equals("active", versionVo.getStatus())) {
            return versionVo;
        }
        String userUuid = UserContext.get().getUserUuid(true);
        inspectConfigCompareMapper.updateBaselineVersionStatusByBaselineId(versionVo.getBaselineId(), "approved", userUuid);
        Date now = new Date();
        versionVo.setStatus("active");
        if (versionVo.getApprovedTime() == null) {
            versionVo.setApprovedTime(now);
        }
        versionVo.setActivatedTime(now);
        versionVo.setLcu(userUuid);
        inspectConfigCompareMapper.updateBaselineVersionToActive(versionVo);
        InspectConfigBaselineVo baselineVo = inspectConfigCompareMapper.getBaselineById(versionVo.getBaselineId());
        if (baselineVo != null) {
            baselineVo.setCurrentVersionId(versionVo.getId());
            baselineVo.setLcu(userUuid);
            inspectConfigCompareMapper.updateBaselineCurrentVersion(baselineVo);
        }
        return getBaselineVersionById(versionVo.getId());
    }

    private void fillBaselineUserVo(List<InspectConfigBaselineVo> baselineList) {
        if (CollectionUtils.isEmpty(baselineList)) {
            return;
        }
        Set<String> userUuidSet = new HashSet<>();
        for (InspectConfigBaselineVo baselineVo : baselineList) {
            if (StringUtils.isNotBlank(baselineVo.getCurrentPublisher())) {
                userUuidSet.add(baselineVo.getCurrentPublisher());
            }
        }
        Map<String, UserVo> userMap = getUserMapByUuidSet(userUuidSet);
        for (InspectConfigBaselineVo baselineVo : baselineList) {
            if (StringUtils.isNotBlank(baselineVo.getCurrentPublisher())) {
                baselineVo.setCurrentPublisherVo(userMap.get(baselineVo.getCurrentPublisher()));
            }
        }
    }

    private void fillVersionUserVo(InspectConfigBaselineVersionVo versionVo) {
        if (versionVo == null) {
            return;
        }
        fillVersionUserVo(Collections.singletonList(versionVo));
    }

    private void fillVersionUserVo(List<InspectConfigBaselineVersionVo> versionList) {
        if (CollectionUtils.isEmpty(versionList)) {
            return;
        }
        Set<String> userUuidSet = new HashSet<>();
        for (InspectConfigBaselineVersionVo versionVo : versionList) {
            if (StringUtils.isNotBlank(versionVo.getApprover())) {
                userUuidSet.add(versionVo.getApprover());
            }
        }
        Map<String, UserVo> userMap = getUserMapByUuidSet(userUuidSet);
        for (InspectConfigBaselineVersionVo versionVo : versionList) {
            if (StringUtils.isNotBlank(versionVo.getApprover())) {
                versionVo.setApproverVo(userMap.get(versionVo.getApprover()));
            }
        }
    }

    private Map<String, UserVo> getUserMapByUuidSet(Set<String> userUuidSet) {
        Map<String, UserVo> userMap = new HashMap<>();
        if (CollectionUtils.isEmpty(userUuidSet)) {
            return userMap;
        }
        List<UserVo> userList = userMapper.getUserByUserUuidList(new ArrayList<>(userUuidSet));
        if (CollectionUtils.isEmpty(userList)) {
            return userMap;
        }
        for (UserVo userVo : userList) {
            if (userVo != null && StringUtils.isNotBlank(userVo.getUuid())) {
                userMap.put(userVo.getUuid(), userVo);
            }
        }
        return userMap;
    }

    public JSONObject compareResource(Long appSystemId, Long appModuleId, Long envId, Long typeId, Long sourceResourceId, Long targetResourceId, String schemaName) {
        InspectConfigSnapshotVo sourceSnapshot = generateSnapshot(appSystemId, appModuleId, envId, typeId, sourceResourceId, schemaName);
        InspectConfigSnapshotVo targetSnapshot = generateSnapshot(appSystemId, appModuleId, envId, typeId, targetResourceId, schemaName);
        return doCompare("peer", appSystemId, appModuleId, envId, sourceSnapshot, null, targetSnapshot);
    }

    private JSONObject doCompare(String compareType, Long appSystemId, Long appModuleId, Long envId, InspectConfigSnapshotVo sourceSnapshot, InspectConfigBaselineVersionVo baselineVersion, InspectConfigSnapshotVo targetSnapshot) {
        JSONObject sourceData = JSONObject.parseObject(sourceSnapshot.getNormalizedData());
        JSONObject targetData = baselineVersion != null ? JSONObject.parseObject(baselineVersion.getBaselineData()) : JSONObject.parseObject(targetSnapshot.getNormalizedData());
        JSONObject compareResult = compareNormalizedData(sourceData, targetData);
        JSONArray diffList = compareResult.getJSONArray("diffList");
        JSONObject summary = buildCompareSummary(compareResult);

        InspectConfigCompareTaskVo taskVo = new InspectConfigCompareTaskVo();
        taskVo.setCompareType(compareType);
        taskVo.setBaselineVersionId(baselineVersion != null ? baselineVersion.getId() : null);
        taskVo.setSourceSnapshotId(sourceSnapshot.getId());
        taskVo.setTargetSnapshotId(targetSnapshot != null ? targetSnapshot.getId() : null);
        taskVo.setAppSystemId(appSystemId);
        taskVo.setAppModuleId(appModuleId);
        taskVo.setEnvId(envId);
        taskVo.setStatus("succeed");
        taskVo.setCompareResult(compareResult.getString("compareResult"));
        taskVo.setRiskLevel(compareResult.getString("riskLevel"));
        taskVo.setIsBlocked(compareResult.getInteger("isBlocked"));
        taskVo.setTotalCount(compareResult.getInteger("totalCount"));
        taskVo.setDiffCount(compareResult.getInteger("diffCount"));
        taskVo.setHighCount(compareResult.getInteger("highCount"));
        taskVo.setMediumCount(compareResult.getInteger("mediumCount"));
        taskVo.setLowCount(compareResult.getInteger("lowCount"));
        taskVo.setSummary(summary.toJSONString());
        String userUuid = UserContext.get().getUserUuid(true);
        taskVo.setFcu(userUuid);
        taskVo.setLcu(userUuid);
        inspectConfigCompareMapper.insertCompareTask(taskVo);

        for (int i = 0; i < diffList.size(); i++) {
            JSONObject diff = diffList.getJSONObject(i);
            InspectConfigCompareDetailVo detailVo = new InspectConfigCompareDetailVo();
            detailVo.setTaskId(taskVo.getId());
            detailVo.setLayer(diff.getString("layer"));
            detailVo.setPath(diff.getString("path"));
            detailVo.setLabel(diff.getString("label"));
            detailVo.setCompareMode(diff.getString("compareMode"));
            detailVo.setStatus(diff.getString("status"));
            detailVo.setRiskLevel(diff.getString("riskLevel"));
            detailVo.setIsBlocked(diff.getInteger("isBlocked"));
            detailVo.setReason(diff.getString("reason"));
            detailVo.setSourceValue(JSON.toJSONString(diff.get("sourceValue")));
            detailVo.setTargetValue(JSON.toJSONString(diff.get("targetValue")));
            detailVo.setSort(i + 1);
            inspectConfigCompareMapper.insertCompareDetail(detailVo);
        }

        JSONObject result = new JSONObject();
        result.put("task", taskVo);
        result.put("diffList", diffList);
        result.put("sourceSnapshot", sourceSnapshot);
        result.put("sourceData", sourceData);
        if (baselineVersion != null) {
            result.put("baselineVersion", baselineVersion);
            result.put("targetData", targetData);
        } else {
            result.put("targetSnapshot", targetSnapshot);
            result.put("targetData", targetData);
        }
        result.put("summary", summary);
        return result;
    }

    private JSONObject getLatestCollectJson(Long resourceId, String schemaName) {
        JSONObject dictionary = mongoTemplate.findOne(new Query(Criteria.where("name").is(schemaName)), JSONObject.class, "_dictionary");
        if (dictionary == null) {
            return null;
        }
        String collectionName = dictionary.getString("collection");
        if (StringUtils.isBlank(collectionName)) {
            return null;
        }
        String mgmtIp = getResourceMgmtIp(resourceId);
        if (StringUtils.isBlank(mgmtIp)) {
            return null;
        }
        String objCategory = StringUtils.removeStart(collectionName, "COLLECT_");
        MongoCollection<Document> collection = mongoTemplate.getCollection(collectionName);
        Document queryDoc = new Document("MGMT_IP", mgmtIp);
        if (StringUtils.isNotBlank(objCategory)) {
            queryDoc.put("_OBJ_CATEGORY", objCategory);
        }
        FindIterable<Document> findIterable = collection.find(queryDoc).sort(new Document("_updatetime", -1)).limit(1);
        Document collectDoc = findIterable.first();
        if (collectDoc == null) {
            return null;
        }
        JSONObject collectJson = JSONObject.parseObject(collectDoc.toJson());
        collectJson.put("fields", dictionary.getJSONArray("fields"));
        collectJson.put("collection", collectionName);
        collectJson.put("label", dictionary.getString("label"));
        return collectJson;
    }

    private String getResourceMgmtIp(Long resourceId) {
        if (resourceId == null) {
            return null;
        }
        IResourceCenterResourceCrossoverService resourceService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
        ResourceVo resourceVo = resourceService.getResourceById(resourceId);
        if (resourceVo == null) {
            return null;
        }
        return resourceVo.getIp();
    }

    private JSONObject normalizeReport(String schemaName, JSONObject reportJson) {
        JSONObject normalized = new JSONObject(true);
        normalized.put("schemaName", schemaName);
        JSONArray fieldList = new JSONArray();
        JSONObject layerData = new JSONObject(true);
        layerData.put("hardware", new JSONObject(true));
        layerData.put("kernel", new JSONObject(true));
        layerData.put("os", new JSONObject(true));
        layerData.put("application", new JSONObject(true));

        Set<String> addedFieldSet = new HashSet<>();
        JSONArray fields = reportJson.getJSONArray("fields");
        if (CollectionUtils.isNotEmpty(fields)) {
            for (int i = 0; i < fields.size(); i++) {
                JSONObject field = fields.getJSONObject(i);
                String fieldName = field.getString("name");
                Object value = reportJson.get(fieldName);
                addField(fieldList, layerData, addedFieldSet, fieldName, field.getString("desc"), field.getString("type"), value);
            }
        }
        for (String key : reportJson.keySet()) {
            if (shouldSkipField(key)) {
                continue;
            }
            addField(fieldList, layerData, addedFieldSet, key, key, inferType(reportJson.get(key)), reportJson.get(key));
        }
        normalized.put("fieldList", fieldList);
        normalized.put("layerData", layerData);
        return normalized;
    }

    private void addField(JSONArray fieldList, JSONObject layerData, Set<String> addedFieldSet, String fieldName, String label, String type, Object value) {
        if (addedFieldSet.contains(fieldName) || shouldSkipField(fieldName) || value == null) {
            return;
        }
        addedFieldSet.add(fieldName);
        Object normalizedValue = normalizeJsonValue(value);
        String layer = getLayer(fieldName);
        JSONObject fieldJson = new JSONObject(true);
        fieldJson.put("path", fieldName);
        fieldJson.put("name", fieldName);
        fieldJson.put("label", StringUtils.defaultIfBlank(label, fieldName));
        fieldJson.put("type", type);
        fieldJson.put("layer", layer);
        fieldJson.put("compareMode", "exact");
        fieldJson.put("value", normalizedValue);
        fieldJson.put("canonicalValue", JSON.toJSONString(normalizedValue));
        fieldList.add(fieldJson);
        layerData.getJSONObject(layer).put(fieldName, normalizedValue);
    }

    private boolean shouldSkipField(String fieldName) {
        if (StringUtils.isBlank(fieldName)) {
            return true;
        }
        return fieldName.startsWith("_") || IGNORE_FIELD_SET.contains(fieldName);
    }

    private String inferType(Object value) {
        if (value instanceof JSONArray) {
            return "JsonArray";
        }
        if (value instanceof JSONObject) {
            return "JsonObject";
        }
        if (value instanceof Integer || value instanceof Long) {
            return "Int";
        }
        if (value instanceof Float || value instanceof Double) {
            return "Float";
        }
        if (value instanceof Boolean) {
            return "Boolean";
        }
        return "String";
    }

    private String getLayer(String fieldName) {
        if (HARDWARE_FIELD_SET.contains(fieldName) || fieldName.startsWith("CPU_") || fieldName.startsWith("MEM_")) {
            return "hardware";
        }
        if (KERNEL_FIELD_SET.contains(fieldName)) {
            return "kernel";
        }
        if (OS_FIELD_SET.contains(fieldName)) {
            return "os";
        }
        return "application";
    }

    private Object normalizeJsonValue(Object value) {
        if (value instanceof JSONObject) {
            JSONObject source = (JSONObject) value;
            JSONObject result = new JSONObject(true);
            List<String> keyList = new ArrayList<>(source.keySet());
            Collections.sort(keyList);
            for (String key : keyList) {
                result.put(key, normalizeJsonValue(source.get(key)));
            }
            return result;
        }
        if (value instanceof JSONArray) {
            JSONArray source = (JSONArray) value;
            List<Object> valueList = new ArrayList<>();
            for (int i = 0; i < source.size(); i++) {
                valueList.add(normalizeJsonValue(source.get(i)));
            }
            valueList.sort(Comparator.comparing(JSON::toJSONString));
            JSONArray result = new JSONArray();
            result.addAll(valueList);
            return result;
        }
        return value;
    }

    private JSONObject buildSummary(JSONObject normalizedData) {
        JSONObject summary = new JSONObject(true);
        JSONArray fieldList = normalizedData.getJSONArray("fieldList");
        summary.put("fieldCount", fieldList != null ? fieldList.size() : 0);
        JSONObject layerCount = new JSONObject(true);
        layerCount.put("hardware", 0);
        layerCount.put("kernel", 0);
        layerCount.put("os", 0);
        layerCount.put("application", 0);
        if (CollectionUtils.isNotEmpty(fieldList)) {
            for (int i = 0; i < fieldList.size(); i++) {
                JSONObject field = fieldList.getJSONObject(i);
                String layer = field.getString("layer");
                layerCount.put(layer, layerCount.getInteger(layer) + 1);
            }
        }
        summary.put("layerCount", layerCount);
        return summary;
    }

    private JSONObject buildCandidateDraft(InspectConfigSnapshotVo snapshotVo) {
        JSONObject aiCandidateDraft = buildAiCandidateDraft(snapshotVo);
        if (aiCandidateDraft != null) {
            return aiCandidateDraft;
        }
        return buildRuleCandidateDraft(snapshotVo);
    }

    private JSONObject buildRuleCandidateDraft(InspectConfigSnapshotVo snapshotVo) {
        return buildRuleCandidateDraft(snapshotVo, null);
    }

    private JSONObject buildRuleCandidateDraft(InspectConfigSnapshotVo snapshotVo, Long analysisDurationMillis) {
        JSONObject normalizedData = JSONObject.parseObject(snapshotVo.getNormalizedData());
        JSONArray fieldList = normalizedData.getJSONArray("fieldList");
        JSONArray includedList = new JSONArray();
        if (CollectionUtils.isNotEmpty(fieldList)) {
            for (int i = 0; i < fieldList.size(); i++) {
                JSONObject field = fieldList.getJSONObject(i);
                JSONObject candidate = new JSONObject(true);
                candidate.put("layer", field.getString("layer"));
                candidate.put("path", field.getString("path"));
                candidate.put("label", field.getString("label"));
                candidate.put("type", field.getString("type"));
                candidate.put("compareMode", field.getString("compareMode"));
                candidate.put("confidence", 0.6D);
                candidate.put("reason", "未命中可用AI模型，按内置规则纳入候选基线草稿");
                includedList.add(candidate);
            }
        }
        JSONObject candidateDraft = new JSONObject(true);
        candidateDraft.put("source", "rule_fallback");
        candidateDraft.put("schemaName", snapshotVo.getSchemaName());
        candidateDraft.put("snapshotId", snapshotVo.getId());
        candidateDraft.put("aiEnabled", 0);
        candidateDraft.put("aiStatus", "fallback");
        candidateDraft.put("message", "未使用AI模型，已按规则生成候选草稿");
        JSONObject candidateSummary = new JSONObject(true);
        candidateSummary.put("originalFieldCount", fieldList != null ? fieldList.size() : 0);
        candidateSummary.put("includedCount", includedList.size());
        candidateSummary.put("excludedCount", 0);
        candidateSummary.put("summary", "未命中可用AI模型，已按规则生成候选草稿");
        candidateSummary.put("includedLayerCount", buildLayerCount(includedList));
        candidateSummary.put("analysisDurationMillis", analysisDurationMillis != null ? analysisDurationMillis : 0L);
        candidateDraft.put("summary", candidateSummary);
        candidateDraft.put("included", includedList);
        candidateDraft.put("excluded", new JSONArray());
        return candidateDraft;
    }

    private JSONObject buildAiCandidateDraft(InspectConfigSnapshotVo snapshotVo) {
        AiModelVo modelVo = getConfiguredChatModel(snapshotVo.getSchemaName());
        if (modelVo == null) {
            return null;
        }
        JSONObject normalizedData = JSONObject.parseObject(snapshotVo.getNormalizedData());
        JSONArray fieldList = normalizedData.getJSONArray("fieldList");
        if (CollectionUtils.isEmpty(fieldList)) {
            return null;
        }
        JSONArray promptFieldList = buildAiPromptFieldList(fieldList);
        if (CollectionUtils.isEmpty(promptFieldList)) {
            return null;
        }
        long startTime = System.currentTimeMillis();
        try {
            List<ChatMessage> messageList = new ArrayList<>();
            messageList.add(SystemMessage.from(buildAiCandidateSystemPrompt(snapshotVo.getSchemaName())));
            messageList.add(UserMessage.from(buildAiCandidateUserPrompt(promptFieldList)));
            JSONObject aiInvokeResult = executeAiCandidatePrompt(modelVo.getId(), messageList);
            String responseText = aiInvokeResult != null ? aiInvokeResult.getString("text") : null;
            JSONObject aiResult = parseAiCandidateResult(responseText);
            JSONObject candidateDraft = mergeAiCandidateResult(
                    snapshotVo,
                    normalizedData,
                    modelVo,
                    aiResult,
                    System.currentTimeMillis() - startTime,
                    aiInvokeResult != null ? aiInvokeResult.getString("callMode") : null
            );
            if (candidateDraft != null) {
                return candidateDraft;
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(),ex);
        }
        return null;
    }

    private InspectConfigAiSettingVo buildAiSettingScope(String schemaName) {
        InspectConfigAiSettingVo settingVo = new InspectConfigAiSettingVo();
        settingVo.setSchemaName(schemaName);
        return settingVo;
    }

    private JSONObject executeAiCandidatePrompt(Long modelId, List<ChatMessage> messageList) throws Exception {
        ChatModel chatModel = AiModelFactory.getChatModel(modelId);
        if (chatModel != null) {
            try {
                ChatResponse response = chatModel.chat(ChatRequest.builder().messages(messageList).build());
                JSONObject result = new JSONObject(true);
                result.put("callMode", AI_CALL_MODE_CHAT);
                result.put("text", response != null && response.aiMessage() != null ? response.aiMessage().text() : "");
                return result;
            } catch (Exception ex) {
                if (!isStreamOnlyModelError(ex)) {
                    throw ex;
                }
            }
        }
        StreamingChatModel streamingChatModel = AiModelFactory.getStreamingChatModel(modelId);
        if (streamingChatModel == null) {
            return null;
        }
        return executeStreamingAiCandidatePrompt(streamingChatModel, messageList);
    }

    private JSONObject executeStreamingAiCandidatePrompt(StreamingChatModel streamingChatModel, List<ChatMessage> messageList) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicReference<ChatResponse> responseRef = new AtomicReference<>();
        StringBuilder textBuilder = new StringBuilder();
        streamingChatModel.chat(ChatRequest.builder().messages(messageList).build(), new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                if (StringUtils.isNotBlank(partialResponse)) {
                    textBuilder.append(partialResponse);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                responseRef.set(completeResponse);
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                errorRef.set(error);
                latch.countDown();
            }
        });
        latch.await();
        if (errorRef.get() != null) {
            throw new Exception(errorRef.get());
        }
        JSONObject result = new JSONObject(true);
        result.put("callMode", AI_CALL_MODE_STREAM);
        if (StringUtils.isNotBlank(textBuilder.toString())) {
            result.put("text", textBuilder.toString());
            return result;
        }
        ChatResponse response = responseRef.get();
        result.put("text", response != null && response.aiMessage() != null ? response.aiMessage().text() : "");
        return result;
    }

    private boolean isStreamOnlyModelError(Throwable ex) {
        if (ex == null || StringUtils.isBlank(ex.getMessage())) {
            return false;
        }
        return StringUtils.containsIgnoreCase(ex.getMessage(), "only support stream mode")
                || StringUtils.containsIgnoreCase(ex.getMessage(), "enable the stream parameter");
    }

    private AiModelVo getConfiguredChatModel(String schemaName) {
        try {
            InspectConfigAiSettingVo settingVo = inspectConfigCompareMapper.getAiSettingByScope(buildAiSettingScope(schemaName));
            if (settingVo != null && settingVo.getModelId() != null) {
                AiModelVo modelVo = aiModelMapper.getAiModelById(settingVo.getModelId());
                if (modelVo != null && Objects.equals(modelVo.getModelType(), AiModelType.CHAT.getValue())) {
                    return modelVo;
                }
            }
            AiModelVo query = new AiModelVo();
            query.setModelType(AiModelType.CHAT.getValue());
            List<AiModelVo> modelList = aiModelMapper.searchAiModel(query);
            if (CollectionUtils.isEmpty(modelList)) {
                return null;
            }
            return modelList.get(0);
        } catch (Exception ignored) {
            return null;
        }
    }

    private JSONArray buildAiPromptFieldList(JSONArray fieldList) {
        JSONArray result = new JSONArray();
        for (int i = 0; i < fieldList.size() && result.size() < AI_FIELD_LIMIT; i++) {
            JSONObject field = fieldList.getJSONObject(i);
            if (field == null || StringUtils.isBlank(field.getString("path"))) {
                continue;
            }
            JSONObject item = new JSONObject(true);
            item.put("name", StringUtils.defaultIfBlank(field.getString("name"), field.getString("path")));
            item.put("label", field.getString("label"));
            item.put("layer", field.getString("layer"));
            item.put("type", field.getString("type"));
            result.add(item);
        }
        return result;
    }

    private String buildAiCandidateSystemPrompt(String schemaName) {
        String configuredPrompt = getConfiguredAiPrompt(schemaName);
        return StringUtils.defaultIfBlank(configuredPrompt, getDefaultAiCandidateSystemPrompt());
    }

    private String buildAiCandidateUserPrompt(JSONArray promptFieldList) {
        JSONObject payload = new JSONObject(true);
        payload.put("fieldList", promptFieldList);
        return "请根据以下fieldList筛选适合进入配置基线草稿的字段，仅返回JSON。\n" + payload.toJSONString();
    }

    private String getConfiguredAiPrompt(String schemaName) {
        if (StringUtils.isBlank(schemaName)) {
            return getDefaultAiCandidateSystemPrompt();
        }
        InspectConfigAiSettingVo settingVo = inspectConfigCompareMapper.getAiSettingByScope(buildAiSettingScope(schemaName));
        if (settingVo == null || StringUtils.isBlank(settingVo.getPrompt())) {
            return getDefaultAiCandidateSystemPrompt();
        }
        return settingVo.getPrompt();
    }

    private String getDefaultAiCandidateSystemPrompt() {
        return DEFAULT_AI_CANDIDATE_SYSTEM_PROMPT;
    }

    private String normalizeAiSettingPrompt(String prompt) {
        String normalizedPrompt = StringUtils.trimToNull(prompt);
        if (normalizedPrompt == null) {
            return null;
        }
        if (StringUtils.equals(normalizedPrompt, getDefaultAiCandidateSystemPrompt())) {
            return null;
        }
        return normalizedPrompt;
    }

    private JSONObject parseAiCandidateResult(String responseText) {
        if (StringUtils.isBlank(responseText)) {
            return null;
        }
        String jsonText = responseText.trim();
        if (StringUtils.startsWith(jsonText, "```")) {
            jsonText = StringUtils.substringAfter(jsonText, "```");
            jsonText = StringUtils.substringAfter(jsonText, "\n");
            jsonText = StringUtils.substringBeforeLast(jsonText, "```").trim();
        }
        if (!StringUtils.startsWithAny(jsonText, "{", "[")) {
            int begin = jsonText.indexOf('{');
            int end = jsonText.lastIndexOf('}');
            if (begin >= 0 && end > begin) {
                jsonText = jsonText.substring(begin, end + 1);
            }
        }
        try {
            return JSONObject.parseObject(jsonText);
        } catch (Exception ex) {
            return null;
        }
    }

    private JSONObject mergeAiCandidateResult(InspectConfigSnapshotVo snapshotVo, JSONObject normalizedData, AiModelVo modelVo, JSONObject aiResult, Long analysisDurationMillis, String callMode) {
        if (aiResult == null) {
            return null;
        }
        Map<String, JSONObject> fieldMap = toFieldMap(normalizedData);
        JSONArray aiIncludedList = aiResult.getJSONArray("included");
        if (CollectionUtils.isEmpty(aiIncludedList)) {
            return null;
        }
        JSONArray includedList = new JSONArray();
        Set<String> includedPathSet = new LinkedHashSet<>();
        for (int i = 0; i < aiIncludedList.size(); i++) {
            JSONObject item = aiIncludedList.getJSONObject(i);
            if (item == null) {
                continue;
            }
            String path = item.getString("path");
            if (StringUtils.isBlank(path) || !fieldMap.containsKey(path) || includedPathSet.contains(path)) {
                continue;
            }
            JSONObject field = fieldMap.get(path);
            JSONObject candidate = new JSONObject(true);
            candidate.put("layer", field.getString("layer"));
            candidate.put("path", field.getString("path"));
            candidate.put("label", field.getString("label"));
            candidate.put("type", field.getString("type"));
            candidate.put("compareMode", StringUtils.defaultIfBlank(item.getString("compareMode"), field.getString("compareMode")));
            candidate.put("confidence", item.getDouble("confidence") != null ? item.getDouble("confidence") : 0.8D);
            candidate.put("reason", StringUtils.defaultIfBlank(item.getString("reason"), "AI建议纳入候选基线"));
            includedList.add(candidate);
            includedPathSet.add(path);
        }
        if (CollectionUtils.isEmpty(includedList)) {
            return null;
        }
        JSONArray excludedList = new JSONArray();
        Set<String> excludedPathSet = new LinkedHashSet<>();
        JSONArray aiExcludedList = aiResult.getJSONArray("excluded");
        if (CollectionUtils.isNotEmpty(aiExcludedList)) {
            for (int i = 0; i < aiExcludedList.size(); i++) {
                JSONObject item = aiExcludedList.getJSONObject(i);
                if (item == null || StringUtils.isBlank(item.getString("path")) || excludedPathSet.contains(item.getString("path"))) {
                    continue;
                }
                JSONObject excluded = new JSONObject(true);
                excluded.put("path", item.getString("path"));
                excluded.put("reason", StringUtils.defaultIfBlank(item.getString("reason"), "AI建议排除"));
                excludedList.add(excluded);
                excludedPathSet.add(item.getString("path"));
            }
        }
        for (Map.Entry<String, JSONObject> entry : fieldMap.entrySet()) {
            String path = entry.getKey();
            if (includedPathSet.contains(path) || excludedPathSet.contains(path)) {
                continue;
            }
            JSONObject excluded = new JSONObject(true);
            excluded.put("path", path);
            excluded.put("reason", "AI未纳入候选基线");
            excludedList.add(excluded);
            excludedPathSet.add(path);
        }
        JSONObject candidateSummary = new JSONObject(true);
        candidateSummary.put("originalFieldCount", fieldMap.size());
        candidateSummary.put("includedCount", includedList.size());
        candidateSummary.put("excludedCount", excludedList.size());
        candidateSummary.put("summary", aiResult.get("summary"));
        candidateSummary.put("includedLayerCount", buildLayerCount(includedList));
        candidateSummary.put("analysisDurationMillis", analysisDurationMillis != null ? analysisDurationMillis : 0L);
        candidateSummary.put("callMode", StringUtils.defaultIfBlank(callMode, AI_CALL_MODE_CHAT));

        JSONObject candidateDraft = new JSONObject(true);
        candidateDraft.put("source", "ai");
        candidateDraft.put("schemaName", snapshotVo.getSchemaName());
        candidateDraft.put("snapshotId", snapshotVo.getId());
        candidateDraft.put("aiEnabled", 1);
        candidateDraft.put("aiStatus", "succeed");
        candidateDraft.put("modelId", modelVo.getId());
        candidateDraft.put("modelName", modelVo.getName());
        candidateDraft.put("modelLabel", modelVo.getModelName());
        candidateDraft.put("callMode", StringUtils.defaultIfBlank(callMode, AI_CALL_MODE_CHAT));
        candidateDraft.put("message", "已使用AI辅助生成候选基线草稿");
        candidateDraft.put("summary", candidateSummary);
        candidateDraft.put("included", includedList);
        candidateDraft.put("excluded", excludedList);
        return candidateDraft;
    }

    private JSONObject buildLayerCount(JSONArray candidateList) {
        JSONObject layerCount = new JSONObject(true);
        layerCount.put("hardware", 0);
        layerCount.put("kernel", 0);
        layerCount.put("os", 0);
        layerCount.put("application", 0);
        if (CollectionUtils.isEmpty(candidateList)) {
            return layerCount;
        }
        for (int i = 0; i < candidateList.size(); i++) {
            JSONObject item = candidateList.getJSONObject(i);
            String layer = item.getString("layer");
            if (layerCount.containsKey(layer)) {
                layerCount.put(layer, layerCount.getInteger(layer) + 1);
            }
        }
        return layerCount;
    }

    private JSONObject buildBaselineDraft(InspectConfigSnapshotVo snapshotVo, JSONObject candidateDraft) {
        JSONObject normalizedData = JSONObject.parseObject(snapshotVo.getNormalizedData());
        Map<String, JSONObject> fieldMap = toFieldMap(normalizedData);
        JSONArray includedList = candidateDraft != null ? candidateDraft.getJSONArray("included") : null;
        if (CollectionUtils.isEmpty(includedList)) {
            return normalizedData;
        }
        JSONArray fieldList = new JSONArray();
        for (int i = 0; i < includedList.size(); i++) {
            JSONObject item = includedList.getJSONObject(i);
            if (item == null || StringUtils.isBlank(item.getString("path"))) {
                continue;
            }
            JSONObject field = fieldMap.get(item.getString("path"));
            if (field == null) {
                continue;
            }
            JSONObject baselineField = new JSONObject(true);
            baselineField.put("path", field.getString("path"));
            baselineField.put("name", field.getString("name"));
            baselineField.put("label", field.getString("label"));
            baselineField.put("type", field.getString("type"));
            baselineField.put("layer", field.getString("layer"));
            baselineField.put("compareMode", StringUtils.defaultIfBlank(item.getString("compareMode"), field.getString("compareMode")));
            baselineField.put("value", field.get("value"));
            fieldList.add(baselineField);
        }
        if (CollectionUtils.isEmpty(fieldList)) {
            return normalizedData;
        }
        JSONObject baselineJson = new JSONObject(true);
        baselineJson.put("schemaName", normalizedData.getString("schemaName"));
        baselineJson.put("fieldList", fieldList);
        return normalizeBaselineDraft(baselineJson);
    }

    private String buildBaselineDraftChangeSummary(Long resourceId, JSONObject candidateDraft) {
        if (candidateDraft == null) {
            return "由资源“" + resourceId + "”当前采集快照生成基线草稿";
        }
        if (Objects.equals(candidateDraft.getInteger("aiEnabled"), 1)) {
            return "由资源“" + resourceId + "”当前采集快照经AI辅助筛选生成基线草稿";
        }
        return "由资源“" + resourceId + "”当前采集快照按规则生成基线草稿";
    }

    private JSONObject normalizeBaselineDraft(JSONObject baselineJson) {
        if (baselineJson == null) {
            throw new ParamNotExistsException("baselineData");
        }
        JSONArray fieldList = baselineJson.getJSONArray("fieldList");
        if (CollectionUtils.isEmpty(fieldList)) {
            throw new ParamNotExistsException("fieldList");
        }
        JSONArray normalizedFieldList = new JSONArray();
        JSONObject layerData = new JSONObject(true);
        layerData.put("hardware", new JSONObject(true));
        layerData.put("kernel", new JSONObject(true));
        layerData.put("os", new JSONObject(true));
        layerData.put("application", new JSONObject(true));
        for (int i = 0; i < fieldList.size(); i++) {
            JSONObject field = fieldList.getJSONObject(i);
            if (field == null || StringUtils.isBlank(field.getString("path"))) {
                continue;
            }
            Object normalizedValue = normalizeJsonValue(field.get("value"));
            String layer = StringUtils.defaultIfBlank(field.getString("layer"), getLayer(field.getString("path")));
            JSONObject normalizedField = new JSONObject(true);
            normalizedField.put("path", field.getString("path"));
            normalizedField.put("name", StringUtils.defaultIfBlank(field.getString("name"), field.getString("path")));
            normalizedField.put("label", StringUtils.defaultIfBlank(field.getString("label"), field.getString("path")));
            normalizedField.put("type", StringUtils.defaultIfBlank(field.getString("type"), inferType(normalizedValue)));
            normalizedField.put("layer", layer);
            normalizedField.put("compareMode", StringUtils.defaultIfBlank(field.getString("compareMode"), "exact"));
            normalizedField.put("value", normalizedValue);
            normalizedField.put("canonicalValue", JSON.toJSONString(normalizedValue));
            normalizedFieldList.add(normalizedField);
            layerData.getJSONObject(layer).put(field.getString("path"), normalizedValue);
        }
        JSONObject normalizedBaseline = new JSONObject(true);
        normalizedBaseline.put("schemaName", baselineJson.getString("schemaName"));
        normalizedBaseline.put("fieldList", normalizedFieldList);
        normalizedBaseline.put("layerData", layerData);
        return normalizedBaseline;
    }

    private JSONObject compareNormalizedData(JSONObject sourceData, JSONObject targetData) {
        Map<String, JSONObject> sourceFieldMap = toFieldMap(sourceData);
        Map<String, JSONObject> targetFieldMap = toFieldMap(targetData);
        Set<String> pathSet = new TreeSet<>();
        pathSet.addAll(sourceFieldMap.keySet());
        pathSet.addAll(targetFieldMap.keySet());
        JSONArray diffList = new JSONArray();
        int highCount = 0;
        int mediumCount = 0;
        int lowCount = 0;
        int diffCount = 0;
        int totalCount = pathSet.size();
        for (String path : pathSet) {
            JSONObject sourceField = sourceFieldMap.get(path);
            JSONObject targetField = targetFieldMap.get(path);
            boolean isDifferent = false;
            String status;
            String reason;
            Object sourceValue = sourceField != null ? sourceField.get("value") : null;
            Object targetValue = targetField != null ? targetField.get("value") : null;
            if (sourceField == null) {
                isDifferent = true;
                status = "missing";
                reason = "源快照缺少该字段";
            } else if (targetField == null) {
                isDifferent = true;
                status = "additional";
                reason = "目标快照缺少该字段";
            } else if (!Objects.equals(sourceField.getString("canonicalValue"), targetField.getString("canonicalValue"))) {
                isDifferent = true;
                status = "different";
                reason = "字段值不一致";
            } else {
                status = "same";
                reason = "字段一致";
            }
            if (isDifferent) {
                diffCount++;
                String layer = sourceField != null ? sourceField.getString("layer") : targetField.getString("layer");
                String riskLevel = getRiskLevel(layer);
                if (Objects.equals("high", riskLevel)) {
                    highCount++;
                } else if (Objects.equals("medium", riskLevel)) {
                    mediumCount++;
                } else {
                    lowCount++;
                }
                JSONObject diff = new JSONObject(true);
                diff.put("path", path);
                diff.put("label", sourceField != null ? sourceField.getString("label") : targetField.getString("label"));
                diff.put("layer", layer);
                diff.put("compareMode", "exact");
                diff.put("status", status);
                diff.put("riskLevel", riskLevel);
                diff.put("isBlocked", Objects.equals("high", riskLevel) ? 1 : 0);
                diff.put("reason", reason);
                diff.put("sourceValue", sourceValue);
                diff.put("targetValue", targetValue);
                diffList.add(diff);
            }
        }
        JSONObject summary = new JSONObject(true);
        summary.put("totalCount", totalCount);
        summary.put("diffCount", diffCount);
        summary.put("highCount", highCount);
        summary.put("mediumCount", mediumCount);
        summary.put("lowCount", lowCount);
        summary.put("compareResult", diffCount == 0 ? "compliant" : "noncompliant");
        summary.put("riskLevel", highCount > 0 ? "high" : (mediumCount > 0 ? "medium" : (lowCount > 0 ? "low" : "none")));
        summary.put("isBlocked", highCount > 0 ? 1 : 0);
        summary.put("diffList", diffList);
        return summary;
    }

    private JSONObject buildCompareSummary(JSONObject compareResult) {
        JSONObject summary = new JSONObject(true);
        summary.put("totalCount", compareResult.getInteger("totalCount"));
        summary.put("diffCount", compareResult.getInteger("diffCount"));
        summary.put("highCount", compareResult.getInteger("highCount"));
        summary.put("mediumCount", compareResult.getInteger("mediumCount"));
        summary.put("lowCount", compareResult.getInteger("lowCount"));
        summary.put("compareResult", compareResult.getString("compareResult"));
        summary.put("riskLevel", compareResult.getString("riskLevel"));
        summary.put("isBlocked", compareResult.getInteger("isBlocked"));
        return summary;
    }

    private Map<String, JSONObject> toFieldMap(JSONObject normalizedData) {
        Map<String, JSONObject> result = new LinkedHashMap<>();
        JSONArray fieldList = normalizedData.getJSONArray("fieldList");
        if (CollectionUtils.isNotEmpty(fieldList)) {
            for (int i = 0; i < fieldList.size(); i++) {
                JSONObject field = fieldList.getJSONObject(i);
                result.put(field.getString("path"), field);
            }
        }
        return result;
    }

    private String getRiskLevel(String layer) {
        if (Objects.equals("hardware", layer) || Objects.equals("kernel", layer)) {
            return "high";
        }
        if (Objects.equals("os", layer)) {
            return "medium";
        }
        return "low";
    }

    private int getFieldCount(String normalizedData) {
        if (StringUtils.isBlank(normalizedData)) {
            return 0;
        }
        JSONObject normalizedJson = JSONObject.parseObject(normalizedData);
        JSONArray fieldList = normalizedJson.getJSONArray("fieldList");
        return fieldList != null ? fieldList.size() : 0;
    }

    private Date extractCollectTime(JSONObject collectJson) {
        JSONObject updateTime = collectJson.getJSONObject("_updatetime");
        if (updateTime != null) {
            Date date = updateTime.getDate("$date");
            if (date != null) {
                return date;
            }
        }
        JSONObject renewTime = collectJson.getJSONObject("_renewtime");
        if (renewTime != null) {
            Date date = renewTime.getDate("$date");
            if (date != null) {
                return date;
            }
        }
        return new Date();
    }

    private String buildScopeHash(Long appSystemId, Long appModuleId, Long envId, Long typeId, String schemaName) {
        return Md5Util.encryptMD5(StringUtils.join(Arrays.asList(
                String.valueOf(appSystemId),
                String.valueOf(appModuleId),
                String.valueOf(envId),
                String.valueOf(typeId),
                schemaName
        ), "_"));
    }
}

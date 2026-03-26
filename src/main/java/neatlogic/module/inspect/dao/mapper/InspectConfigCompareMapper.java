package neatlogic.module.inspect.dao.mapper;

import neatlogic.module.inspect.dto.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface InspectConfigCompareMapper {

    List<InspectConfigBaselineVo> getBaselineList(InspectConfigBaselineVo searchVo);

    InspectConfigBaselineVo getBaselineByScopeHash(String scopeHash);

    InspectConfigBaselineVo getBaselineById(Long id);

    InspectConfigAiSettingVo getAiSettingByScope(InspectConfigAiSettingVo settingVo);

    InspectConfigBaselineVersionVo getBaselineVersionById(Long id);

    List<InspectConfigBaselineVersionVo> getBaselineVersionListByBaselineId(Long baselineId);

    InspectConfigSnapshotVo getSnapshotById(Long id);

    InspectConfigSnapshotVo getLatestSnapshot(@Param("appSystemId") Long appSystemId,
                                              @Param("appModuleId") Long appModuleId,
                                              @Param("envId") Long envId,
                                              @Param("resourceId") Long resourceId,
                                              @Param("typeId") Long typeId,
                                              @Param("schemaName") String schemaName);

    void insertBaseline(InspectConfigBaselineVo baselineVo);

    void insertAiSetting(InspectConfigAiSettingVo settingVo);

    void updateAiSetting(InspectConfigAiSettingVo settingVo);

    void updateBaselineCurrentVersion(InspectConfigBaselineVo baselineVo);

    void insertBaselineVersion(InspectConfigBaselineVersionVo versionVo);

    void updateBaselineVersionContent(InspectConfigBaselineVersionVo versionVo);

    void updateBaselineVersionToActive(InspectConfigBaselineVersionVo versionVo);

    void updateBaselineVersionStatusById(InspectConfigBaselineVersionVo versionVo);

    void updateBaselineVersionStatusByBaselineId(@Param("baselineId") Long baselineId,
                                                 @Param("status") String status,
                                                 @Param("lcu") String lcu);

    void deleteCompareDetailByBaselineVersionId(Long baselineVersionId);

    void deleteCompareTaskByBaselineVersionId(Long baselineVersionId);

    void deleteBaselineVersionById(Long id);

    int getBaselineVersionCountByBaselineId(Long baselineId);

    void deleteCompareDetailByBaselineId(Long baselineId);

    void deleteCompareTaskByBaselineId(Long baselineId);

    void deleteBaselineRuleByBaselineId(Long baselineId);

    void deleteBaselineVersionByBaselineId(Long baselineId);

    void deleteBaselineById(Long id);

    void insertSnapshot(InspectConfigSnapshotVo snapshotVo);

    void insertCompareTask(InspectConfigCompareTaskVo taskVo);

    void updateCompareTask(InspectConfigCompareTaskVo taskVo);

    void insertCompareDetail(InspectConfigCompareDetailVo detailVo);

    List<InspectConfigCompareDetailVo> getCompareDetailListByTaskId(Long taskId);
}

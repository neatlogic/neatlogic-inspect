/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.service;

import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.crossover.IFileCrossoverService;
import codedriver.framework.inspect.dto.InspectResourceConfigurationFilePathVo;
import codedriver.framework.inspect.dto.InspectResourceConfigurationFileVersionVo;
import codedriver.module.inspect.dao.mapper.InspectConfigurationFileMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InspectConfigurationFileServiceImpl implements InspectConfigurationFileService {

    @Resource
    private InspectConfigurationFileMapper inspectConfigurationFileMapper;

    @Override
    public void clearFile(List<Long> resourceIdList, List<InspectResourceConfigurationFilePathVo> inspectResourceConfigurationFilePathList) throws Exception {
        if (CollectionUtils.isNotEmpty(inspectResourceConfigurationFilePathList)) {
            List<Long> idList = inspectResourceConfigurationFilePathList.stream().map(InspectResourceConfigurationFilePathVo::getId).collect(Collectors.toList());
            List<InspectResourceConfigurationFileVersionVo> inspectResourceConfigurationFileVersionList = inspectConfigurationFileMapper.getInspectResourceConfigurationFileVersionListByPathIdList(idList);
            if (CollectionUtils.isNotEmpty(inspectResourceConfigurationFileVersionList)) {
                IFileCrossoverService fileCrossoverService = CrossoverServiceFactory.getApi(IFileCrossoverService.class);
                for (InspectResourceConfigurationFileVersionVo fileVersionVo : inspectResourceConfigurationFileVersionList) {
                    fileCrossoverService.deleteFile(fileVersionVo.getFileId(), null);
                }
            }
            inspectConfigurationFileMapper.deleteResourceConfigFileRecordByPathIdList(idList);
            inspectConfigurationFileMapper.deleteResourceConfigFileVersionByPathIdList(idList);
            inspectConfigurationFileMapper.resetInspectResourceConfigurationFilePathFileInfoByIdList(idList);
        }
    }
}

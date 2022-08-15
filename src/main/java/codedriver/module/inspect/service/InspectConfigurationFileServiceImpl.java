/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.inspect.service;

import codedriver.framework.common.util.FileUtil;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.crossover.IFileCrossoverService;
import codedriver.framework.exception.file.FileNotFoundException;
import codedriver.framework.file.dao.mapper.FileMapper;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.inspect.dto.InspectResourceConfigurationFilePathVo;
import codedriver.framework.inspect.dto.InspectResourceConfigurationFileVersionVo;
import codedriver.framework.lcs.BaseLineVo;
import codedriver.module.inspect.dao.mapper.InspectConfigurationFileMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InspectConfigurationFileServiceImpl implements InspectConfigurationFileService {

    private final static Logger logger = LoggerFactory.getLogger(InspectConfigurationFileServiceImpl.class);
    @Resource
    private InspectConfigurationFileMapper inspectConfigurationFileMapper;

    @Autowired
    private FileMapper fileMapper;

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

    @Override
    public List<BaseLineVo> getLineList(Long fileId) {
        FileVo fileVo = fileMapper.getFileById(fileId);
        if (fileVo == null) {
            throw new FileNotFoundException(fileId);
        }
        List<BaseLineVo> lineList = new ArrayList<>();
        try (InputStream inputStream = FileUtil.getData(fileVo.getPath());
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            int lineNumber = 1;
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                BaseLineVo lineVo = new BaseLineVo(lineNumber, "p", line);
                lineList.add(lineVo);
                lineNumber++;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return lineList;
    }
}

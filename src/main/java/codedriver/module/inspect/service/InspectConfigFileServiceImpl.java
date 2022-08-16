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
import codedriver.framework.inspect.dto.InspectConfigFilePathVo;
import codedriver.framework.inspect.dto.InspectConfigFileVersionVo;
import codedriver.framework.lcs.BaseLineVo;
import codedriver.framework.lcs.constvalue.LineHandler;
import codedriver.module.inspect.dao.mapper.InspectConfigFileMapper;
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
public class InspectConfigFileServiceImpl implements InspectConfigFileService {

    private final static Logger logger = LoggerFactory.getLogger(InspectConfigFileServiceImpl.class);
    @Resource
    private InspectConfigFileMapper inspectConfigFileMapper;

    @Autowired
    private FileMapper fileMapper;

    @Override
    public void clearFile(List<Long> resourceIdList, List<InspectConfigFilePathVo> inspectResourceConfigFilePathList) throws Exception {
        if (CollectionUtils.isNotEmpty(inspectResourceConfigFilePathList)) {
            List<Long> idList = inspectResourceConfigFilePathList.stream().map(InspectConfigFilePathVo::getId).collect(Collectors.toList());
            List<InspectConfigFileVersionVo> inspectResourceConfigFileVersionList = inspectConfigFileMapper.getInspectConfigFileVersionListByPathIdList(idList);
            if (CollectionUtils.isNotEmpty(inspectResourceConfigFileVersionList)) {
                IFileCrossoverService fileCrossoverService = CrossoverServiceFactory.getApi(IFileCrossoverService.class);
                for (InspectConfigFileVersionVo fileVersionVo : inspectResourceConfigFileVersionList) {
                    fileCrossoverService.deleteFile(fileVersionVo.getFileId(), null);
                }
            }
            inspectConfigFileMapper.deleteInspectConfigFileAuditByPathIdList(idList);
            inspectConfigFileMapper.deleteInspectConfigFileVersionByPathIdList(idList);
            inspectConfigFileMapper.resetInspectConfigFilePathFileInfoByIdList(idList);
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
                BaseLineVo lineVo = new BaseLineVo(lineNumber, LineHandler.TEXT.getValue(), line);
                lineList.add(lineVo);
                lineNumber++;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return lineList;
    }
}

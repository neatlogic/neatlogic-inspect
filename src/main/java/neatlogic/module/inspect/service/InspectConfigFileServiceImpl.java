/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.inspect.service;

import neatlogic.framework.common.util.FileUtil;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.crossover.IFileCrossoverService;
import neatlogic.framework.exception.file.FileNotFoundException;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.inspect.dto.InspectConfigFilePathVo;
import neatlogic.framework.inspect.dto.InspectConfigFileVersionVo;
import neatlogic.framework.lcs.BaseLineVo;
import neatlogic.framework.lcs.constvalue.LineHandler;
import neatlogic.module.inspect.dao.mapper.InspectConfigFileMapper;
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

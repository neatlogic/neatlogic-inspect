/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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

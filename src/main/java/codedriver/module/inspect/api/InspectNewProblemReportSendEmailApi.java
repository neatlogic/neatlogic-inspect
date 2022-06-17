package codedriver.module.inspect.api;

import codedriver.framework.asynchronization.thread.CodeDriverThread;
import codedriver.framework.asynchronization.threadpool.CachedThreadPool;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.ICiCrossoverMapper;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.inspect.auth.INSPECT_BASE;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.service.UserService;
import codedriver.framework.util.EmailUtil;
import codedriver.module.inspect.service.InspectReportService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author laiwt
 * @date 2022/5/17 2:40 下午
 */
@Service
@AuthAction(action = INSPECT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class InspectNewProblemReportSendEmailApi extends PrivateApiComponentBase {

    final Logger logger = LoggerFactory.getLogger(InspectNewProblemReportSendEmailApi.class);

    final Integer receiverLimit = 100;

    @Resource
    UserMapper userMapper;

    @Resource
    UserService userService;

    @Resource
    private InspectReportService inspectReportService;

    @Override
    public String getName() {
        return "发送巡检最新问题报告邮件";
    }

    @Override
    public String getToken() {
        return "inspect/new/problem/report/sendemail";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "receiverList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "收件人"),
            @Param(name = "title", type = ApiParamType.STRING, xss = true, isRequired = true, desc = "邮件标题"),
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "模糊搜索"),
            @Param(name = "typeId", type = ApiParamType.LONG, isRequired = true, desc = "类型id"),
            @Param(name = "protocolIdList", type = ApiParamType.JSONARRAY, desc = "协议id列表"),
            @Param(name = "stateIdList", type = ApiParamType.JSONARRAY, desc = "状态id列表"),
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, desc = "环境id列表"),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "应用系统id列表"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "资源类型id列表"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, desc = "标签id列表"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显的资源ID列表"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "巡检状态列表"),
            @Param(name = "inspectJobPhaseNodeStatusList", type = ApiParamType.JSONARRAY, desc = "巡检作业状态列表"),
            @Param(name = "isNeedAlertDetail", type = ApiParamType.ENUM, rule = "1,0", desc = "1:会导出具体告警信息；0：不会导出具体告警信息;默认为0"),
    })
    @Description(desc = "发送巡检最新问题报告邮件")
    @Override
    @ResubmitInterval(5)
    public Object myDoService(JSONObject paramObj) throws Exception {
        String title = paramObj.getString("title");
        ResourceSearchVo searchVo = JSON.toJavaObject(paramObj, ResourceSearchVo.class);
        Integer isNeedAlertDetail = paramObj.getInteger("isNeedAlertDetail");
        if (isNeedAlertDetail == null) {
            isNeedAlertDetail = 0;
        }
        Long typeId = searchVo.getTypeId();
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo ciVo = ciCrossoverMapper.getCiById(typeId);
        if (ciVo == null) {
            throw new CiNotFoundException(typeId);
        }
        searchVo.setLft(ciVo.getLft());
        searchVo.setRht(ciVo.getRht());
        searchVo.setPageSize(100);
        searchVo.setCurrentPage(1);
        Workbook workbook = inspectReportService.getInspectNewProblemReportWorkbook(searchVo, isNeedAlertDetail);
        if (workbook != null) {
            List<String> receiverList = paramObj.getJSONArray("receiverList").toJavaList(String.class);
            Set<String> userUuidList = new HashSet<>();
            Set<String> teamUuidList = new HashSet<>();
            Set<String> emailList = new HashSet<>();
            for (String receiver : receiverList) {
                String[] split = receiver.split("#");
                String type = split[0];
                String uuid = split[1];
                if (GroupSearch.USER.getValue().equals(type)) {
                    userUuidList.add(uuid);
                } else if (GroupSearch.TEAM.getValue().equals(type)) {
                    teamUuidList.add(uuid); // 不穿透查询
                } else if (GroupSearch.ROLE.getValue().equals(type)) {
                    userUuidList.addAll(userMapper.getUserUuidListByRoleUuid(uuid));
                    // 查询角色关联的组，如果组有穿透，则穿透查询
                    Set<String> teamUuidSet = userService.getTeamUuidSetByRoleUuid(uuid);
                    if (teamUuidSet != null) {
                        teamUuidList.addAll(teamUuidSet);
                    }
                }
            }
            if (userUuidList.size() > 0) {
                emailList.addAll(userMapper.getActiveUserEmailListByUserUuidList(new ArrayList<>(userUuidList)));
            }
            if (emailList.size() > receiverLimit) {
                emailList = emailList.stream().limit(receiverLimit).collect(Collectors.toSet());
                logger.error("巡检最新问题报告邮件收件人数量超过{}", receiverLimit);
            } else {
                if (teamUuidList.size() > 0) {
                    for (String teamUuid : teamUuidList) {
                        emailList.addAll(userMapper.getActiveUserEmailListByTeamUuid(teamUuid));
                        if (emailList.size() > receiverLimit) {
                            emailList = emailList.stream().limit(receiverLimit).collect(Collectors.toSet());
                            logger.error("巡检最新问题报告邮件收件人数量超过{}", receiverLimit);
                            break;
                        }
                    }
                }
            }
            // 发送邮件耗时过长，另开线程执行
            Set<String> finalEmailList = emailList;
            CachedThreadPool.execute(new CodeDriverThread(this.getClassName() + "-Thread") {
                @Override
                protected void execute() {
                    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                        if (finalEmailList.size() > 0) {
                            workbook.write(os);
                            InputStream is = new ByteArrayInputStream(os.toByteArray());
                            Map<String, InputStream> attachmentMap = new HashMap<>();
                            attachmentMap.put(title + ".xlsx", is);
                            EmailUtil.sendEmailWithFile(title, title, String.join(",", finalEmailList), null, attachmentMap);
                            is.close();
                        }
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
            });
        }
        return null;
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

}
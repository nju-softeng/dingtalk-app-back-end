package com.softeng.dingtalk.component.dingApi;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiProcessInstanceTerminateRequest;
import com.dingtalk.api.request.OapiProcessinstanceCreateRequest;
import com.dingtalk.api.request.OapiProcessinstanceGetRequest;
import com.dingtalk.api.response.OapiProcessInstanceTerminateResponse;
import com.dingtalk.api.response.OapiProcessinstanceCreateResponse;
import com.dingtalk.api.response.OapiProcessinstanceGetResponse;
import com.softeng.dingtalk.entity.AbsentOA;
import com.softeng.dingtalk.entity.User;
import com.softeng.dingtalk.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class OAApi extends BaseApi{
    @Autowired
    UserService userService;
    @Value("${OA.askForLeaveProcessCode}")
    private String  absentOAProcessCode;
    public String createAbsentOA(AbsentOA absentOAPO) {
        try {
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/processinstance/create");
            OapiProcessinstanceCreateRequest request= new OapiProcessinstanceCreateRequest();
            request.setAgentId(AGENTID);
            //设置process code
            request.setProcessCode(absentOAProcessCode);
            request.setOriginatorUserId(userService.getUserid(absentOAPO.getUser().getId()));
            request.setDeptId(-1L);
            //设置表单内容
            List<OapiProcessinstanceCreateRequest.FormComponentValueVo> form = new ArrayList<>();
            OapiProcessinstanceCreateRequest.FormComponentValueVo type = new OapiProcessinstanceCreateRequest.FormComponentValueVo();
            form.add(type);
            type.setName("请假类型");
            type.setValue(absentOAPO.getType());
            OapiProcessinstanceCreateRequest.FormComponentValueVo start = new OapiProcessinstanceCreateRequest.FormComponentValueVo();
            form.add(start);
            start.setName("开始时间");
            start.setValue(absentOAPO.getDingTalkSchedule().getStart().toString());
            OapiProcessinstanceCreateRequest.FormComponentValueVo end = new OapiProcessinstanceCreateRequest.FormComponentValueVo();
            form.add(end);
            end.setName("结束时间");
            end.setValue(absentOAPO.getDingTalkSchedule().getEnd().toString());
//            OapiProcessinstanceCreateRequest.FormComponentValueVo dayNum = new OapiProcessinstanceCreateRequest.FormComponentValueVo();
//            form.add(dayNum);
//            dayNum.setName("结束时间");
//            dayNum.setValue(absentOA.getDayNum().toString());
            OapiProcessinstanceCreateRequest.FormComponentValueVo reason = new OapiProcessinstanceCreateRequest.FormComponentValueVo();
            form.add(reason);
            reason.setName("请假缘由");
            reason.setValue(absentOAPO.getReason());
            request.setFormComponentValues(form);
            //设置审批人
            List<OapiProcessinstanceCreateRequest.ProcessInstanceApproverVo> processInstanceApproverVoList = new ArrayList<>();
            OapiProcessinstanceCreateRequest.ProcessInstanceApproverVo processInstanceApproverVo = new OapiProcessinstanceCreateRequest.ProcessInstanceApproverVo();
            processInstanceApproverVoList.add(processInstanceApproverVo);
            request.setApprovers(absentOAPO.getDingTalkSchedule().getOrganizer().getUserid());
            OapiProcessinstanceCreateResponse rsp = client.execute(request, getAccessToken());
            return rsp.getProcessInstanceId();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,e.getMessage());
        }
    }
    public int getOAOutCome(String processInstanceId) {
        try {
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/processinstance/get");
            OapiProcessinstanceGetRequest req = new OapiProcessinstanceGetRequest();
            req.setProcessInstanceId(processInstanceId);
            OapiProcessinstanceGetResponse rsp = client.execute(req, getAccessToken());
            if(rsp.getProcessInstance().getStatus().equals("COMPLETED")){
                if(rsp.getProcessInstance().getResult().equals("agree"))return 1;
                else return 0;
            } else return -1;
        } catch (Exception e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,e.getMessage());
        }

    }
    public boolean deleteAbsentOA(String processInstanceId, User user){
        try {
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/process/instance/terminate");
            OapiProcessInstanceTerminateRequest req = new OapiProcessInstanceTerminateRequest();
            OapiProcessInstanceTerminateRequest.TerminateProcessInstanceRequestV2 processInstanceRequestV2 = new OapiProcessInstanceTerminateRequest.TerminateProcessInstanceRequestV2();
            processInstanceRequestV2.setProcessInstanceId(processInstanceId);
            processInstanceRequestV2.setIsSystem(false);
            processInstanceRequestV2.setRemark("取消请假");
            processInstanceRequestV2.setOperatingUserid(user.getUserid());
            req.setRequest(processInstanceRequestV2);
            OapiProcessInstanceTerminateResponse rsp = client.execute(req,getAccessToken());
            return rsp.getResult();
        } catch (Exception e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,e.getMessage());
        }
    }
}

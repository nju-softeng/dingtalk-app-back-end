package com.softeng.dingtalk.controller;

import com.softeng.dingtalk.entity.PaperLevel;
import com.softeng.dingtalk.entity.PatentLevel;
import com.softeng.dingtalk.entity.SubsidyLevel;
import com.softeng.dingtalk.entity.User;
import com.softeng.dingtalk.service.SystemService;
import com.softeng.dingtalk.vo.QueryUserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * @author zhanyeye
 * @description
 * @create 5/27/2020 4:49 PM
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class SystemController {
    @Autowired
    SystemService systemService;


    /**
     * 根据条件分页筛选用户
     * @param vo
     * @param page
     * @return
     */
    @PostMapping("/system/user/{page}")
    public Map<String, Object> queryUser(@RequestBody QueryUserVO vo, @PathVariable int page) {
//        Page<User> pages = systemService.multiQueryUser(page, 10, vo.getName(), vo.getPosition());
//        return Map.of("content", pages.getContent(), "total", pages.getTotalElements());
        return systemService.multiQueryUser(page, 10, vo.getName(), vo.getPosition());
    }


    /**
     * 从钉钉同步用户
     */
    @GetMapping("/system/fetchuser")
    public void fetchUser() {
        systemService.fetchUsers();
    }


    /**
     * 查询所有的绩效标准
     * @return List<SubsidyLevel>
     */
    @GetMapping("/system/subsidy")
    public List<SubsidyLevel> listSubsidy() {
        return systemService.listSubsidy();
    }


    /**
     * 更新绩效标准
     * @param subsidyLevels
     */
    @PostMapping("/system/subsidy")
    public void updateSubsidy(@RequestBody List<SubsidyLevel> subsidyLevels) {
        systemService.setSubsidy(subsidyLevels);
    }


    /**
     * 查询所有的论文AC标准
     * @return
     */
    @GetMapping("/system/paperlevel")
    public List<PaperLevel> listPaperLevel() {
        return systemService.listPaperLevel();
    }

    /**
     * 查询所有的专利AC标准
     * @return List<PatentLevel>
     */
    @GetMapping("/system/patentLevel")
    public List<PatentLevel> listPatentLevel() {
        return systemService.listPatentLevel();
    }

    /**
     * 更新专利AC标准
     * @param patentLevels
     */
    @PostMapping("/system/patentLevel")
    public void updatePatentLevel(@RequestBody List<PatentLevel> patentLevels) {
        systemService.updatePatentLevel(patentLevels);
    }
    /**
     * 更新论文绩效标准
     * @param paperLevels
     */
    @PostMapping("/system/paperlevel")
    public void updatePaperLevel(@RequestBody List<PaperLevel> paperLevels) {
        systemService.updatePaperLevel(paperLevels);
    }


    /**
     * 更新用户信息
     * @param user
     */
    @PostMapping("/system/userinfo")
    public void updateUserInfo(@RequestBody User user) {
        systemService.updateUserInfo(user);
    }


    /**
     * 禁用用户
     * @param uid
     */
    @GetMapping("/system/disable/user/{uid}")
    public void disableUser(@PathVariable int uid) {
        systemService.disableUser(uid);
    }


    /**
     * 取消禁用用户
     * @param uid
     */
    @GetMapping("/system/enable/user/{uid}")
    public void enableUser(@PathVariable int uid) {
        systemService.enableUser(uid);
    }


    /**
     * 查询禁用的用户
     * @return
     */
    @GetMapping("/system/query/disableuser")
    public List<User> queryDisableUser() {
        return systemService.queryDisableUser();
    }

    /**
     * 手动更新指定月份的 DcSummary
     * @param yearmonth 要更新绩效的月份
     */
    @GetMapping("/system/updateperf/{yearmonth}")
    public void manulUpdatePerformance(@PathVariable int yearmonth) {
        systemService.manualUpdatePerformance(yearmonth);
    }

    /**
     * 手动指定某天，扣除当天未提交周报的博士 硕士分数
     */
    @PostMapping("/system/weekreport/acrecord")
    public void manulDeductedPointsUnsubmittedWeeklyReport(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startTime) {
        systemService.manualDeductedPointsUnSubmittedWeeklyReport(startTime);
    }

    /**
     * 手动指定某天，向当天未提交周报的博士 硕士发送提醒消息
     */
    @PostMapping("/system/weekreport/reminder")
    public void manualReminderToSubmitWeeklyReport(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startTime) {
        systemService.manualReminderToSubmitWeeklyReport(startTime);
    }

}

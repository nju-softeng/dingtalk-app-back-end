package com.softeng.dingtalk.component;

import com.softeng.dingtalk.component.dingApi.MessageApi;
import com.softeng.dingtalk.dao.repository.*;
import com.softeng.dingtalk.enums.Position;
import com.softeng.dingtalk.enums.PracticeStateEnum;
import com.softeng.dingtalk.entity.*;
import com.softeng.dingtalk.service.*;
import com.softeng.dingtalk.utils.stuNumUtils.StuNumParser;
import com.softeng.dingtalk.utils.stuNumUtils.StuNumParserFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author zhanyeye
 * @description 定时器
 * @create 2/9/2020 12:30 PM
 */

@Slf4j
@Component
@Transactional
public class Timer {

    @Autowired
    VoteRepository voteRepository;
    @Autowired
    VoteService voteService;
    @Autowired
    InternalPaperRepository internalPaperRepository;
    @Autowired
    InitService initService;
    @Autowired
    ExternalPaperRepository externalPaperRepository;
    @Autowired
    WeeklyReportService weeklyReportService;
    @Autowired
    AcRecordRepository acRecordRepository;

    @Autowired
    MessageApi messageApi;
    @Autowired
    SystemService systemService;

    @Resource
    private PracticeRepository practiceRepository;
    @Resource
    private UserRepository userRepository;

    /**
     * 每周周日23点扫描一次，给当天还未提交周报的博士、硕士发送提醒消息
     */
    @Scheduled(cron = "0 0 23 ? * SUN")
    public void weeklyReportAndPerformanceFillingReminder() {
        log.info(LocalDate.now() + "定时扫描提醒周报和绩效填写");
        systemService.manualReminderToSubmitWeeklyReport(LocalDate.now());
    }

    /**
     * 每周一凌晨2点扫描一次，查询周日一整天没有提交周报的博士、硕士，每人扣 1 ac
     */
    @Scheduled(cron = "0 0 2 ? * MON")
    public void deductedPointsUnSubmittedWeeklyReport() {
        log.info(LocalDate.now() + " 定时扫描扣分");
        systemService.manualDeductedPointsUnSubmittedWeeklyReport(LocalDate.now().minusDays(1));
    }

    //每天凌晨扫描一次,会议的AC结算
    @Scheduled(cron = "0 0 0 * * ?")
    public void calculateScheduleAC(){
        log.info(LocalDate.now() + " 定时扫描会议未参加者的扣分");
        systemService.calculateScheduleAC();
    }

    /**
     * 每20s扫描一次，看是否有待启动的投票
     */
    @Scheduled(cron = "0/20 * * * * ?")
    public void checkVote() {
        LocalDateTime now = LocalDateTime.now();

        // (针对外部评审投票) 检查是否有投票需要开始
        List<Vote> upcomingVotes = voteService.listUpcomingVote(now);
        upcomingVotes.forEach(v -> {
            // 标注该投票已经开始
            v.setStarted(true);
            voteRepository.save(v);

            ExternalPaper externalPaper = externalPaperRepository.findByVid(v.getId());
            if (externalPaper != null) {
                // 发送投票开始的消息
                String markdown = startVoteInfo(externalPaper.getTitle(), v.getEndTime());
                String url = generateVoteDetailUrl(true, externalPaper.getId());
                messageApi.sendActionCard("外部评审投票", markdown, "前往投票", url);
            }
        });
    }

    /**
     * 每20s扫描一次，看是否有待结束的投票
     */
    @Scheduled(cron = "0/20 * * * * ?")
    public void checkVote2() {
        // 检查是否有需要被结束的投票
        LocalDateTime now = LocalDateTime.now();

        List<Vote> votes = voteService.listUnderwayVote(now);
        votes.forEach(v -> {
            // 更新汇总投票结果
            v = voteService.updateVote(v);
            Paper paper = v.isExternal() ?
                    externalPaperRepository.findByVid(v.getId()) :
                    internalPaperRepository.findByVid(v.getId());
            messageApi.sendActionCard(
                    "投票结果",
                    voteResultInfo(paper.getTitle(), v.getResult(), v.getAccept(), v.getTotal()),
                    "查看详情",
                    generateVoteDetailUrl(paper.isExternal(), paper.getId())
            );
        });
    }

    /**
     * 每月1日3点执行 initDcDummary 方法
     */
    @Scheduled(cron = "0 0 3 1 * ?")
    public void initMonthlyDcSummary() {
        initService.initDcSummary();
    }

    /**
     * 投票消息卡片跳转的 url
     *
     * @param isExternal 是否是外部论文
     * @param pid        论文id
     * @return
     */
    private String generateVoteDetailUrl(boolean isExternal, int pid) {
        return (isExternal ? "/paper/ex-detail/" : "/paper/in-detail/") + pid + "/vote";
    }

    /**
     * 确认投票结果的消息模板
     *
     * @param title     论文标题
     * @param result    投票预测结果
     * @param acceptCnt accept数量
     * @param totalCnt  总投票数量
     * @return
     */
    private String voteResultInfo(String title, int result, int acceptCnt, int totalCnt) {
        return new StringBuilder().append(" #### 投票结果 \n ##### 论文： ").append(title)
                .append(" \n 最终结果： ").append(result == 1 ? "accept" : result == 0 ? "reject" : "flat")
                .append("  \n  Accept: ").append(acceptCnt).append(" 票  \n ")
                .append("Reject: ").append(totalCnt - acceptCnt).append(" 票  \n ")
                .append("已参与人数： ").append(totalCnt).append("人  \n ").toString();
    }

    /**
     * 发起投票的消息模板
     *
     * @param title    论文标题
     * @param dateTime 截至时间
     * @return
     */
    private String startVoteInfo(String title, LocalDateTime dateTime) {
        return " #### 投票 \n ##### 论文： " + title + " \n 截止时间: " + dateTime.toLocalTime().toString();
    }


//    @Scheduled(cron = "0/20 * * * * ?")
//    public void test() {
//        log.info(LocalDate.now() + "test");
//    }

    /**
     * 每天凌晨一点根据审核通过的实习申请修改对应用户的工作状态
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void editWorkState() {
        log.info("每天凌晨一点根据审核通过的实习申请修改对应用户的工作状态");
        List<Practice> practiceList = practiceRepository.findAllByStateEquals(PracticeStateEnum.ACCEPTED.getValue());
        LocalDate cur = LocalDate.now();
        practiceList.forEach(practice -> {
            LocalDate start = practice.getStart();
            LocalDate end = practice.getEnd();
            User user = practice.getUser();
            user.setWorkState((cur.isAfter(start) || cur.isEqual(start)) && (cur.isBefore(end) || cur.isEqual(end)));
            userRepository.save(user);
        });
    }

    /**
     * 每月22日15点10分进行用户职位的变更
     */
    @Scheduled(cron = "0 10 15 22 * ?")
    public void updateUserPosition() {
        log.info("每月22日15点10分进行用户职位的变更");
        userRepository.findAllValidUser()
                .stream()
//                筛选出设置了学号的用户
                .filter((user -> user.getStuNum() != null))
                .forEach((user -> {
                    //                排除教师用户
                    if(user.getPosition() == Position.TEACHER || user.getPosition() == Position.DOCTOR) return;
                    StuNumParser parser= StuNumParserFactory.generateParser(user.getStuNum().trim().length());
                    assert parser != null;
                    user.setPosition(parser.parse(user.getStuNum()));
                    userRepository.save(user);
                }));
    }

}

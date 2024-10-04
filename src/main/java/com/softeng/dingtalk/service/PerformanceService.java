package com.softeng.dingtalk.service;

import com.softeng.dingtalk.dao.repository.AcRecordRepository;
import com.softeng.dingtalk.dao.repository.BugRepository;
import com.softeng.dingtalk.dao.repository.DcSummaryRepository;
import com.softeng.dingtalk.dao.repository.UserRepository;
import com.softeng.dingtalk.entity.AcRecord;
import com.softeng.dingtalk.entity.DcSummary;
import com.softeng.dingtalk.enums.Position;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author zhanyeye
 * @description 绩效、助研金相关服务
 * @create 2/6/2020 9:48 AM
 */
@Service
@Transactional
@Slf4j
public class PerformanceService {
    @Autowired
    DcSummaryRepository dcSummaryRepository;
    @Autowired
    AcRecordRepository acRecordRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    BugRepository bugRepository;
    @Autowired
    SystemService systemService;


    /**
     * 计算指定月份某用户的助研金
     * 计算助研金 Gain = Base * DC * (1+ (AC/100)) + topup
     * @param uid 用户id
     * @param yearmonth 指定年月
     */
    public void computeSalary(int uid, int yearmonth) {
        // 获取当前月份的总dc值
        double dc = dcSummaryRepository.getDcTotal(uid, yearmonth);
        // 获取到目前为止用户的AC总和
        double ac = acRecordRepository.getUserAcByDate(uid, yearmonth);
        // 获取用户的 topup
        double topup = dcSummaryRepository.findTopup(uid, yearmonth);
        Position position = userRepository.getUserPosition(uid);
        double base = systemService.getSubsidy(position);
        double salary = Math.round(base * dc * (1 + (ac/100)) + topup);
        dcSummaryRepository.updateSalary(uid, yearmonth, ac, topup, salary);
        log.debug("uid: {}, salary: {}", uid, salary);
    }

    public void computeSalary(int uid, LocalDate date) {
        int yearmonth = date.getYear() * 100 + date.getMonthValue();
        computeSalary(uid, yearmonth);
    }

    /**
     * 更新指定用户指定月份的topup
     * @param uid
     * @param yearmonth
     * @param topup
     */
    public void updateTopup(int uid, int yearmonth, double topup) {
        DcSummary dcSummary = Optional.ofNullable(dcSummaryRepository.getDcSummary(uid, yearmonth))
                .orElse(new DcSummary(uid, yearmonth));
        dcSummary.setTopup(topup);
        dcSummaryRepository.save(dcSummary);
        computeSalary(uid, yearmonth);
    }


    /**
     * 拿到用户所有 AC 日志
     * @param uid
     * @return
     */
    public List<Map<String, Object>> listUserAc(int uid) {
        return acRecordRepository.listUserAc(uid);
    }


    /**
     * 实验室所有人的 DC 汇总（指定月份）
     * 查询指定月份的绩效汇总
     * @param date
     * @return
     */
    public List<Map<String, Object>> listDcSummaryVO(LocalDate date, boolean isDesc) {
        int yearmonth = date.getYear() * 100 + date.getMonthValue();
        systemService.manualUpdatePerformance(yearmonth);
        return isDesc ?
                dcSummaryRepository.listDcSummaryDesc(yearmonth) :
                dcSummaryRepository.listDcSummaryAsc(yearmonth);
    }


    /**
     * 实验室所有人的 AC 汇总
     * @return
     */
    public List<Map<String, Object>> listAcSummary() {
        return acRecordRepository.listAcSummary();
    }


    /**
     * 拿到最近10条 AC 变动
     * @return
     */
    public List<Map<String, Object>> listLastAc() {
        return acRecordRepository.listLastAc();
    }


    /**
     * 用首页显示的绩效信息
     * @param uid
     * @return
     */
    public Map getUserPerformance(int uid) {
        LocalDate date  = LocalDate.now();
        int yearmonth = date.getYear() * 100 + date.getMonthValue();
        DcSummary dc = Optional.ofNullable(dcSummaryRepository.findByUserIdAndYearmonth(uid, yearmonth))
                .orElse(new DcSummary());
        return Map.of(
                "acTotal", acRecordRepository.getUserAcSum(uid),
                "dcTotal", dc.getTotal(),
                "w1", dc.getWeek1(),
                "w2", dc.getWeek2(),
                "w3", dc.getWeek3(),
                "w4", dc.getWeek4(),
                "w5", dc.getWeek5());
    }

//    todo
    public void addPerformance(AcRecord acRecord) {
        acRecordRepository.save(acRecord);
    }

}

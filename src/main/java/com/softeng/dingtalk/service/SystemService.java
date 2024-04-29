package com.softeng.dingtalk.service;

import com.dingtalk.api.response.OapiUserGetResponse;
import com.softeng.dingtalk.component.convertor.UserConvertor;
import com.softeng.dingtalk.component.dingApi.*;
import com.softeng.dingtalk.component.AcAlgorithm;
import com.softeng.dingtalk.constant.LocalUrlConstant;
import com.softeng.dingtalk.dao.repository.*;
import com.softeng.dingtalk.dto.resp.UserResp;
import com.softeng.dingtalk.entity.*;
import com.softeng.dingtalk.enums.Position;
import com.softeng.dingtalk.utils.StreamUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author zhanyeye
 * @description
 * @create 5/27/2020 4:31 PM
 */
@Service
@Transactional
@Slf4j
public class SystemService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    SubsidyLevelRepository subsidyLevelRepository;
    @Autowired
    PaperLevelRepository paperLevelRepository;
    @Autowired
    DcSummaryRepository dcSummaryRepository;
    @Autowired
    DingTalkScheduleRepository dingTalkScheduleRepository;
    @Autowired
    PerformanceService performanceService;
    @Autowired
    AuditService auditService;

    @Autowired
    ContactsApi contactsApi;
    @Autowired
    ReportApi reportApi;
    @Autowired
    MessageApi messageApi;
    @Autowired
    ScheduleApi scheduleApi;
    @Autowired
    OAApi oaApi;

    @Autowired
    WeeklyReportService weeklyReportService;
    @Autowired
    AcRecordRepository acRecordRepository;
    @Autowired
    PatentLevelRepository patentLevelRepository;

    @Resource
    UserConvertor userConvertor;

    @Value("${DingTalkSchedule.absentACPunishment}")
    double absentACPunishment;

    /**
     * 根据钉钉 userId 获取系统 uid，前端JSAPI选人
     * @param userId
     * @return
     */
    public int getIdByUserid(String userId) {
        Integer id = userRepository.findIdByUserid(userId);
        return id != null ? id : addNewUser(userId).getId();
    }


    /**
     * 从钉钉服务器拉取所有用户同步到系统
     */
    public void fetchUsers() {
        contactsApi.listDepid().stream()
                .flatMap(depId -> contactsApi.listUserId(depId).stream())
                .forEach(this::addNewUser);
    }


    /**
     * 添加新用户
     * @param userid
     * @return
     */
    public User addNewUser(String userid) {
        OapiUserGetResponse response =  contactsApi.fetchUserDetail(userid);

        // 权限
        int authority;
        if (response.getIsBoss()) {
            // 是否为企业的老板
            authority = User.ADMIN_AUTHORITY;
        } else if (response.getIsLeaderInDepts().contains("true")) {
            // 是否为管理员
            authority = User.AUDITOR_AUTHORITY;
        } else {
            authority = User.NORMAL_AUTHORITY;
        }

        // 职位
        Position position;
        switch (Optional.ofNullable(response.getPosition()).orElse("")){
            case "本": position = Position.UNDERGRADUATE; break;
            case "学硕": position = Position.ACADEMIC;  break;
            case "专硕": position = Position.PROFESSIONAL;  break;
            case "博": position = Position.DOCTOR;  break;
//            todo-
            default:   position = Position.OTHER;  break;
        }

        User user = Optional.ofNullable(userRepository.findByUserid(userid))
                .orElse(
                        new User(
                                response.getUserid(),
                                response.getUnionid(),
                                response.getName(),
                                response.getAvatar(),
                                authority,
                                position
                        )
                );
        user.setAvatar(response.getAvatar());
        user.setName(response.getName());
        user.setUnionid(response.getUnionid());
        if(user.getPosition() == Position.OTHER) {
            switch (Optional.ofNullable(user.getStuNum()).orElse("--").substring(0, 2)) {
                case "MF":
                case "mf":
                    user.setPosition(Position.PROFESSIONAL);
                    break;
                case "mg":
                case "MG":
                    user.setPosition(Position.ACADEMIC);
                    break;
                case "DG":
                case "dg":
                    user.setPosition(Position.DOCTOR);
                    break;
                default:
                    break;
            }
        }
        return userRepository.save(user);
    }


    /**
     * 多条件查询用户信息
     * 单不是依据 name 查询时，进行缓存
     * @param page
     * @param size
     * @param name
     * @param position
     * @return
     */
    public Map<String, Object> multiQueryUser(int page, int size, String name, String position) {
        Specification<User> spec = (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("deleted"), false));
//          todo-修改

//            predicates.add(criteriaBuilder.notEqual(root.get("authority"), User.ADMIN_AUTHORITY));
            if (!"".equals(name)) {
                // 根据姓名模糊查询
                predicates.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
            }
            if (!"".equals(position)) {
                // 根据学位模糊查询
                Position eposition = Stream.of(Position.values())
                        .filter(c -> c.getTitle().equals(position))
                        .findFirst()
                        .orElseThrow(IllegalArgumentException::new);
                predicates.add(criteriaBuilder.equal(root.get("position"), eposition));
            }
            Predicate[] arr = new Predicate[predicates.size()];
            return criteriaBuilder.and(predicates.toArray(arr));
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
//        return userRepository.findAll(spec, pageable);
        Page<User> userPage = userRepository.findAll(spec, pageable);
        List<UserResp> userRespList = StreamUtils.map( userPage.toList(), user -> userConvertor.entity2Resp(user));
        return Map.of("content", userRespList, "total", userPage.getTotalElements());
    }


    /**
     * 查询绩效标准
     * @param position
     * @return double
     */
    @Cacheable(value="subsidy", key = "#position")
    public double getSubsidy(Position position) {
        return subsidyLevelRepository.getSubsidy(position);
    }


    /**
     * 更新绩效标准
     * @param subsidyLevelList
     */
    @CacheEvict(value = "subsidy", allEntries = true)
    public void setSubsidy(List<SubsidyLevel> subsidyLevelList) {
        for (SubsidyLevel sl : subsidyLevelList) {
            subsidyLevelRepository.updateSubsidy(sl.getPosition(), sl.getSubsidy());
        }
    }


    /**
     * 获取所有的绩效标准
     * @return List<SubsidyLevel>
     */
    public List<SubsidyLevel> listSubsidy() {
        return subsidyLevelRepository.findAll();
    }


    /**
     * 查询论文AC标准
     * @return
     */
    public List<PaperLevel> listPaperLevel() {
        return paperLevelRepository.findAll();
    }

    /**
     * 查询专利AC标准
     * @return
     */
    public List<PatentLevel> listPatentLevel() {
        return patentLevelRepository.findAll();
    }


    /**
     * 更新论文AC标准
     * @param paperLevels
     */
    public void updatePaperLevel(List <PaperLevel> paperLevels) {
        for (PaperLevel pl : paperLevels) {
            paperLevelRepository.updatePaperLevel(pl.getPaperType(), pl.getTotal());
        }
    }

    /**
     * 更新专利AC标准
     * @param patentLevels
     */
    public void updatePatentLevel(List <PatentLevel> patentLevels) {
        for (PatentLevel pl : patentLevels) {
            patentLevelRepository.updatePatentLevel(pl.getTitle(), pl.getTotal());
        }
    }


    /**
     * 更新用户信息
     * @param u
     */
    public void updateUserInfo(User u) {
        log.debug(u.toString());
        userRepository.updateUserInfo(u.getId(), u.getStuNum(), u.getPosition(), u.getAuthority());
    }


    /**
     * 停用用户，学生毕业之后它的信息不应该出现在报表中
     * @param uid
     */
    public void disableUser(int uid) {
        userRepository.disableUser(uid);
    }


    /**
     * 恢复用户
     * @param uid
     */
    public void enableUser(int uid) {
        userRepository.enableUser(uid);
    }


    /**
     * 查询停用的用户
     * @return
     */
    public List<User> queryDisableUser() {
        return userRepository.listDisableUser();
    }

    /**
     * 手动重新计算所有可用用户的绩效
     * 当从数据库录入dc数据时，并不会触发更新 dcsummary
     * 所以需要调用该函数刷新 dcsummary
     */
    public void manualUpdatePerformance(int yearmonth) {
        // todo 拿到所有用户可用 uid
        List<Integer> uidlist = userRepository.listUid();
        // todo 根据用户id,和指定年月更新dcsummary
        for (Integer id : uidlist) {
            auditService.updateDcSummary(id, yearmonth, 1);
            auditService.updateDcSummary(id, yearmonth, 2);
            auditService.updateDcSummary(id, yearmonth, 3);
            auditService.updateDcSummary(id, yearmonth, 4);
            auditService.updateDcSummary(id, yearmonth, 5);
        }
    }

    /**
     * 手动指定某天，当天未交周报的硕士博士扣除相应的分数
     */
    public void manualDeductedPointsUnSubmittedWeeklyReport(LocalDate localDate) {
        var start = localDate.atTime(0, 0, 0);
        var end = start.plusDays(1);
        var poorGuys = weeklyReportService.queryUnSubmittedWeeklyReportUser(start, end);
        if(poorGuys.size() == 0) {
            log.info(start.toString() + "没有未提交周报的poor guy");
            return;
        }
        log.info(start.toString() + "未提交周报扣分" + Arrays.toString(poorGuys.stream().map(User::getName).toArray()));
        acRecordRepository.saveAll(
                poorGuys.stream()
                        // 筛选出在校的用户(或者未设置工作状态的用户)
                        .filter(user -> user.getWorkState() == null || !user.getWorkState())
                        .map(user -> AcRecord.builder()
                                .user(user)
                                .ac(AcAlgorithm.getPointOfUnsubmittedWeekReport(user))
                                .classify(AcRecord.NORMAL)
                                .reason(String.format(
                                        "%s 未按时提交周报",
                                        start.toLocalDate().toString()
                                ))
                                .createTime(end)
                                .build()
                        )
                        .collect(Collectors.toList())
        );
    }

    /**
     * 手动指定某一天，向当天未提交周报的博士硕士发送提醒消息
     * @param localDate
     */
    public void manualReminderToSubmitWeeklyReport(LocalDate localDate) {
        var start = localDate.atTime(0, 0, 0);
        var end = start.plusDays(1);
        messageApi.sendLinkMessage(
                "周报、绩效填写提醒",
                LocalUrlConstant.FRONTEND_PERFORMANCE_URL,
                "您还未提交本周周报，请在周日24点前提交周报并随后申请绩效",
                weeklyReportService.queryUnSubmittedWeeklyReportUser(start, end).stream()
                        // 筛选出在校的用户(或者未设置工作状态的用户)
                        .filter(user -> user.getWorkState() == null || !user.getWorkState())
                        .map(User::getUserid)
                        .collect(Collectors.toList())
        );
    }

    public void calculateScheduleAC() {
        List<DingTalkSchedule> dingTalkScheduleList =dingTalkScheduleRepository.getDingTalkSchedulesByAcCalculatedFalse();
        for(DingTalkSchedule dingTalkSchedule : dingTalkScheduleList){
            LocalDateTime now=LocalDateTime.now();
            if(now.compareTo(dingTalkSchedule.getEnd())>=0){
                //获取改日程请假列表，并获取oa通过的同学的列表
                List<String> osNotPassUserIdList=new LinkedList<>();
                dingTalkSchedule.getAbsentOAList().forEach(absentOA -> {
                    absentOA.setPass(oaApi.getOAOutCome(absentOA.getProcessInstanceId())==1);
                    if(!absentOA.isPass()) osNotPassUserIdList.add(absentOA.getUser().getUserid());
                });
                //获取需要扣分的userId
                List<String> absentUserIdList=scheduleApi.getAbsentList(dingTalkSchedule);
                absentUserIdList.removeAll(osNotPassUserIdList);
                //进行扣分
                if(absentUserIdList.size()!=0){
                    List<DingTalkScheduleDetail> detailList= dingTalkSchedule.getDingTalkScheduleDetailList();
                    detailList.forEach(detail -> {
                        boolean isContain = absentUserIdList .stream().anyMatch(x-> x.equals(detail.getUser().getUnionid()));
                        if(isContain) {
                            AcRecord acRecord =new AcRecord(detail.getUser(),
                                    null,
                                    -1*absentACPunishment,
                                    detail.getDingTalkSchedule().getStart().toString()+"会议缺席",
                                    6,LocalDateTime.now());
                            acRecordRepository.save(acRecord);
                            detail.setAcRecord(acRecord);
                        }
                    });
                }
                dingTalkSchedule.setAcCalculated(true);
                dingTalkScheduleRepository.save(dingTalkSchedule);
            }
        }
    }

}

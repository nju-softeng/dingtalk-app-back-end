package com.softeng.dingtalk.dao.repository;

import com.softeng.dingtalk.entity.AcRecord;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @author zhanyeye
 * @description
 * @date 12/27/2019
 */
@Repository
public interface AcRecordRepository extends CustomizedRepository<AcRecord, Integer> {

    /**
     * 获取所有用户的AC日志
     * @param uid
     * @return
     */
    @Query(value = "SELECT a.reason, a.ac, a.create_time, u.name as auditor, a.classify FROM ac_record a LEFT JOIN user u on a.auditor_id = u.id where user_id = :uid  order by a.create_time desc", nativeQuery = true)
    List<Map<String, Object>> listUserAc(@Param("uid") int uid);

    /**
     * 获取指定用户的总ac
     * @param uid
     * @return
     */
    @Query(value = "select ifnull((select sum(ac) from ac_record where user_id = :uid), 0)", nativeQuery = true)
    Double getUserAcSum(@Param("uid") int uid);

    /**
     * 所有用户的总AC
     * @return
     */
    @Query(value = "select u.id, u.name, ifnull(sum(a.ac), 0)  as total from" +
            " (SELECT id, name FROM `user` WHERE position != '待定' and position != '教师' and is_deleted = 0) u left join" +
            " ac_record a on u.id = a.user_id  group by u.id order by total DESC", nativeQuery = true)
    List<Map<String, Object>> listAcSummary();

    /**
     * 获取用户指定日期前的 AC和，用于计算绩效
     * @param uid
     * @param yearmonth
     * @return
     */
    @Query(value = "select ifnull((select sum(ac) from ac_record where user_id = :uid and DATE_FORMAT(`create_time`,'%Y%m') <= :yearmonth), 0)", nativeQuery = true)
    Double getUserAcByDate(@Param("uid") int uid, @Param("yearmonth") int yearmonth);

    /**
     * 拿到最近10条 AC 变动
     * @return
     */
    @Query(value = "select ac, reason, classify, create_time, u1.name as username, u2.name as auditorname from user u1 right join ac_record a on u1.id = a.user_id left join user u2 on a.auditor_id = u2.id order by a.id desc limit 10", nativeQuery = true)
    List<Map<String, Object>> listLastAc();

}

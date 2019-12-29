package com.softeng.dingtalk.repository;

import com.softeng.dingtalk.entity.DcRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author zhanyeye
 * @description
 * @date 12/12/2019
 */
@Repository
public interface DcRecordRepository extends CustomizedRepository<DcRecord, Integer>, JpaRepository<DcRecord, Integer> {
    /**
     * 获取指定用户，指定周的，在各组的dc之和
     * @param uid 用户ID
     * @param date 所在年月
     * @param week 该月第几周
     * @return java.lang.Integer
     * @Date 9:48 PM 12/29/2019
     **/
    @Query(value = "select sum(dc) from dc_record where user_id = :uid and EXTRACT(YEAR_MONTH from create_time) = :date and week = :week", nativeQuery = true)
    Integer getUserWeekTotalDc(@Param("uid") int uid, @Param("date") String date, @Param("week") int week);
}

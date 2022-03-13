package com.softeng.dingtalk.entity;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @description:
 * @author: zhanyeye
 * @create: 2020-10-21 20:07
 **/
@Entity
@Data
@NoArgsConstructor
@Deprecated
public class ExternalPaper implements Paper {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    /**
     * 论文标题
     */
    private String title;
    /**
     * 该外部论文的最终录用结果, -1为还没有结果, 0为拒绝, 1为接受, 2为中止
     */
    private int result = -1;

    @OneToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(unique = true)
    private Vote vote;

    /**
     * 更新时间
     */
    private LocalDate updateDate;

    /**
     * 插入时间
     */
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", updatable = false, insertable = false)
    private LocalDateTime insertTime;

    /**
     *评审版本文件名称
     */
    private String reviewFileName;

    /**
     *评审版本文件Id
     */
    String reviewFileId;

    /**
     *提交版本文件名称
     */
    String submissionFileName;

    /**
     *提交版本文件Id
     */
    String submissionFileId;

    /**
     *发表版本文件名称
     */
    String publishedFileName;

    /**
     *发表版本文件Id
     */
    String publishedFileId;

    /**
     *发表版本Latex文件名称
     */
    String publishedLatexFileName;

    /**
     *发表版本Latex文件Id
     */
    String publishedLatexFileId;

    public ExternalPaper(String title) {
        this.title = title;
    }

    @Override
    public boolean isExternal() {
        return true;
    }
}

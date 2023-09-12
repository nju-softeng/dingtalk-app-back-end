package com.softeng.dingtalk.component.convertor;

import com.softeng.dingtalk.dto.req.NewsReq;
import com.softeng.dingtalk.dto.resp.NewsResp;
import com.softeng.dingtalk.entity.News;
import com.softeng.dingtalk.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class NewsConvertor extends AbstractConvertorTemplate<NewsReq, NewsResp, News>{

    @Override
    public News req2Entity(NewsReq newsReq) {
        News res = super.req2Entity(newsReq);
        res.setAuthor(new User().setId(newsReq.getAuthorId()));
        res.setIsDeleted(0);
        res.setIsShown(1);
//        这里加上时间，因为News有release_time字段，且为null，
//        会让数据库表的default CURRENT_TIMESTAMP失效
        res.setReleaseTime(LocalDateTime.now());
        return res;
    }

    @Override
    public NewsResp entity2Resp(News news) {
        return super.entity2Resp(news)
                .setAuthorName(news.getAuthor().getName())
                .setAuthorId(news.getAuthor().getId());
    }
}

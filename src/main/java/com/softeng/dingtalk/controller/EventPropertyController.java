package com.softeng.dingtalk.controller;

import com.alibaba.fastjson.JSONObject;
import com.softeng.dingtalk.entity.EventProperty;
import com.softeng.dingtalk.service.EventPropertyService;
import com.softeng.dingtalk.vo.EventPropertyInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
public class EventPropertyController {
    @Autowired
    EventPropertyService eventPropertyService;


    @GetMapping("/event/page/{page}/{size}")
    public List<EventPropertyInfoVO> getEventInfoList(@PathVariable int page, @PathVariable int size){
        return eventPropertyService.getEventInfoList(page,size);
    }

    @PostMapping("/event")
    public void addEventProperty(@RequestParam List<MultipartFile> pictureFileList, @RequestParam List<MultipartFile> videoFileList,
                                 @RequestParam List<MultipartFile> docFileList,
                                 @RequestParam String eventPropertyJsonStr,
                                 @RequestAttribute int uid){
        EventProperty eventProperty= JSONObject.parseObject(eventPropertyJsonStr,EventProperty.class);
        eventPropertyService.addEventProperty(eventProperty,pictureFileList,videoFileList,docFileList,uid);
    }

    @DeleteMapping("/event/{id}")
    public void deleteEventProperty(@PathVariable int id){
        eventPropertyService.deleteEventProperty(id);
    }
}

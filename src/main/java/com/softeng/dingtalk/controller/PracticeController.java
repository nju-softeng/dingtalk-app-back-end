package com.softeng.dingtalk.controller;

import com.softeng.dingtalk.service.PracticeService;
import com.softeng.dingtalk.vo.PracticeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
public class PracticeController {
    @Autowired
    PracticeService practiceService;
    @PostMapping("/practice")
    public void addPractice(@RequestBody PracticeVO practiceVO, @RequestAttribute int uid){
        if(practiceVO.getId()==null){
            practiceService.addPractice(practiceVO,uid);
        } else {
            practiceService.modifyPractice(practiceVO);
        }
    }
    @GetMapping("/practice")
    public void getPracticeList(@RequestAttribute int uid){
        practiceService.getPracticeList(uid);
    }


    @DeleteMapping("/practice/{id}")
    public void deletePractice(@PathVariable int id,@RequestAttribute int uid){
        practiceService.deletePractice(id,uid);
    }
}

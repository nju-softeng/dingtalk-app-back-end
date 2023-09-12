package com.softeng.dingtalk.service;

import com.softeng.dingtalk.entity.Dissertation;
import com.softeng.dingtalk.dao.repository.DissertationPropertyRepository;
import com.softeng.dingtalk.dao.repository.UserRepository;
import com.softeng.dingtalk.vo.DissertationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class DissertationPropertyService {
    @Autowired
    DissertationPropertyRepository dissertationPropertyRepository;
    @Autowired
    FileService fileService;
    @Autowired
    UserRepository userRepository;

    public void addDissertation(MultipartFile file, DissertationVO dissertationVO){
        String fileId=fileService.addFileByPath(file,dissertationVO.getFilePath()+"/PreRejoin");
        Dissertation dissertation =new Dissertation(dissertationVO.getState(),dissertationVO.getGraduateYear(),dissertationVO.getFilePath());
        dissertation.setUser(userRepository.findById(dissertationVO.getUserId()).get());
        dissertation.setPreRejoinFileName(file.getOriginalFilename());
        dissertation.setPreRejoinFileId(fileId);
        dissertationPropertyRepository.save(dissertation);
    }

    public void updateDissertation(DissertationVO dissertationVO){
        Dissertation dissertationRec =dissertationPropertyRepository.findById(dissertationVO.getId()).get();
        dissertationRec.update(dissertationVO.getState(),dissertationVO.getGraduateYear());
        dissertationPropertyRepository.save(dissertationRec);
    }

    public Map<String, Object> getDissertation(int page, int size){
        Pageable pageable = PageRequest.of(page-1,size, Sort.by("id").descending());
        Page<Dissertation> dissertations=dissertationPropertyRepository.findAll(pageable);
        List<Dissertation> dissertationList =dissertations.stream().collect(Collectors.toList());
        return Map.of("list", dissertationList,"total",dissertations.getTotalElements());
    }

    public Dissertation getDissertationDetail(@PathVariable int uid){
        Dissertation dissertationRec =dissertationPropertyRepository.findByUserId(uid);
        return dissertationRec;
    }

    public void deleteDissertation(int id){
        Dissertation dissertation =dissertationPropertyRepository.findById(id).get();
        if(dissertation.getPreRejoinFileId()!=null)fileService.deleteFileByPath(dissertation.getPreRejoinFileName(), dissertation.getPreRejoinFileId());
        if(dissertation.getReviewFileId()!=null)fileService.deleteFileByPath(dissertation.getReviewFileName(), dissertation.getReviewFileId());
        if(dissertation.getRejoinFileId()!=null)fileService.deleteFileByPath(dissertation.getRejoinFileName(), dissertation.getRejoinFileId());
        if(dissertation.getFinalFileId()!=null)fileService.deleteFileByPath(dissertation.getFinalFileName(), dissertation.getFinalFileId());
        dissertationPropertyRepository.deleteById(id);
    }

    public void addDissertationFile(MultipartFile file, String type, int id){
        Dissertation dissertation =dissertationPropertyRepository.findById(id).get();
        String fileId=this.fileService.addFileByPath(file, dissertation.getFilePath()+"/"+getFileTypeFolderName(type));
        String fileName=file.getOriginalFilename();
        switch (type){
            case "preRejoinFile":
                dissertation.setPreRejoinFileId(fileId);
                dissertation.setPreRejoinFileName(fileName);
                break;
            case "reviewFile":
                dissertation.setReviewFileId(fileId);
                dissertation.setReviewFileName(fileName);
                break;
            case "rejoinFile":
                dissertation.setRejoinFileId(fileId);
                dissertation.setRejoinFileName(fileName);
                break;
            case "finalFile":
                dissertation.setFinalFileId(fileId);
                dissertation.setFinalFileName(fileName);
                break;
        }
        dissertation.setState(getDissertationState(dissertation));
        dissertationPropertyRepository.save(dissertation);
    }

    private int getDissertationState(Dissertation dissertation){
        int state=3;
        if(dissertation.getFinalFileId()!=null) return state;
        state--;
        if(dissertation.getRejoinFileId()!=null) return state;
        state--;
        if(dissertation.getReviewFileId()!=null) return state;
        return 0;
    }
    public void deleteDissertationFile(int id,String type){
        Dissertation dissertation =dissertationPropertyRepository.findById(id).get();
        String fileId=null;
        String fileName=null;
        switch (type){
            case "preRejoinFile":
                fileId= dissertation.getPreRejoinFileId();
                fileName= dissertation.getPreRejoinFileName();
                dissertation.setPreRejoinFileName(null);
                dissertation.setPreRejoinFileId(null);
                dissertation.setState(0);
                break;
            case "reviewFile":
                fileId= dissertation.getReviewFileId();
                fileName= dissertation.getReviewFileName();
                dissertation.setReviewFileName(null);
                dissertation.setReviewFileId(null);
                dissertation.setState(0);
                break;
            case "rejoinFile":
                fileId= dissertation.getRejoinFileId();
                fileName= dissertation.getRejoinFileName();
                dissertation.setRejoinFileName(null);
                dissertation.setRejoinFileId(null);
                dissertation.setState(1);
                break;
            case "finalFile":
                fileId= dissertation.getFinalFileId();
                fileName= dissertation.getFinalFileName();
                dissertation.setFinalFileName(null);
                dissertation.setFinalFileId(null);
                dissertation.setState(2);
                break;
        }
        fileService.deleteFileByPath(fileName,fileId);
        dissertationPropertyRepository.save(dissertation);
    }

    public void downLoadDissertationFile(int id,String type,HttpServletResponse response)  {
        DissertationFileInfo dissertationFileInfo=this.getDissertationFileInfo(id,type);
        String fileName=dissertationFileInfo.name;
        String fileId=dissertationFileInfo.id;
        try{
            fileService.downloadFile(fileName,fileId,response);
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

    private DissertationFileInfo getDissertationFileInfo(int id,String type){
        Dissertation dissertation =dissertationPropertyRepository.findById(id).get();
        String fileName=null;
        String fileId=null;
        switch (type){
            case "preRejoinFile":
                fileId= dissertation.getPreRejoinFileId();
                fileName= dissertation.getPreRejoinFileName();
                break;
            case "reviewFile":
                fileId= dissertation.getReviewFileId();
                fileName= dissertation.getReviewFileName();
                break;
            case "rejoinFile":
                fileId= dissertation.getRejoinFileId();
                fileName= dissertation.getRejoinFileName();
                break;
            case "finalFile":
                fileId= dissertation.getFinalFileId();
                fileName= dissertation.getFinalFileName();
                break;
        }
        return new DissertationFileInfo(fileName, fileId);
    }


    static class DissertationFileInfo{
        public String name;
        public String id;
        DissertationFileInfo(String name,String id){
            this.name=name;
            this.id=id;
        }
    }

    private String getFileTypeFolderName(String fileType){
        String res=fileType.substring(0,1).toUpperCase()+fileType.substring(1,fileType.length()-4);
        return res;
    }
}

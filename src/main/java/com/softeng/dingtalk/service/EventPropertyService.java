package com.softeng.dingtalk.service;
import com.softeng.dingtalk.po_entity.EventFile;
import com.softeng.dingtalk.po_entity.EventProperty;
import com.softeng.dingtalk.dao.repository.EventPropertyRepository;
import com.softeng.dingtalk.dao.repository.EventFileRepository;
import com.softeng.dingtalk.vo.EventPropertyInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class EventPropertyService {
    @Autowired
    EventPropertyRepository eventPropertyRepository;
    @Autowired
    EventFileRepository eventFileRepository;
    @Autowired
    FileService fileService;
    @Autowired
    UserService userService;


    /**
     * 获取资产列表
     * @param page
     * @param size
     * @return
     */
    public Map<String, Object> getEventInfoList(int page, int size){
        Pageable pageable = PageRequest.of(page-1,size, Sort.by("id").descending());
        Page<EventProperty> eventProperties=eventPropertyRepository.findAll(pageable);
        List<EventPropertyInfoVO> infoList=eventProperties.stream().map(eventProperty -> new EventPropertyInfoVO(eventProperty.getId(),
                eventProperty.getName(), eventProperty.getYear(), eventProperty.getType())).collect(Collectors.toList());
        return Map.of("list",infoList,"total",eventProperties.getTotalElements());
    }

    public EventProperty getEventInfo(int eventId){
        EventProperty ep = eventPropertyRepository.findById(eventId).get();
        //以下三行代码非常关键，不能删除
        log.info(String.valueOf(ep.getPictureFileList().size()));
        log.info(String.valueOf(ep.getVideoFileList().size()));
        log.info(String.valueOf(ep.getDocFileList().size()));
        return ep;
//        EventProperty ep=eventPropertyRepository.findById(eventId).get();
//        return new EventPropertyInfoVO(ep.getId(),ep.getName(),ep.getYear(),ep.getType());
    }

    /**
     * 添加新的event资产
     * @param eventProperty
     */
    public void addEventProperty(EventProperty eventProperty){
        eventPropertyRepository.save(eventProperty);
    }

    /**
     * 更新event资产
     * @param eventProperty
     */
    public void updateEventProperty(EventProperty eventProperty){
        EventProperty eventPropertyRec =eventPropertyRepository.findById(eventProperty.getId()).get();
        eventPropertyRec.update(eventProperty.getName(), eventProperty.getYear(), eventProperty.getType());
        eventPropertyRepository.save(eventPropertyRec);
    }


    /**
     * 为资产添加文件列表（既可以添加全新文件列表，也可以附加新的文件）
     * @param fileList
     * @param type
     * @param eventId
     */
    public void addEventPropertyFileList(List<MultipartFile> fileList, String type, int eventId){
        EventProperty eventProperty =eventPropertyRepository.findById(eventId).get();
        List<EventFile> eventFileList =saveFileList(fileList, eventProperty, eventProperty.getPath()+"/"+type,type);
        switch (type){
            case "Picture":
                eventFileList =this.appendList(eventProperty.getPictureFileList(), eventFileList);
                eventProperty.setPictureFileList(eventFileList);
                break;
            case "Video":
                eventFileList =this.appendList(eventProperty.getVideoFileList(), eventFileList);
                eventProperty.setVideoFileList(eventFileList);
                break;
            case "Doc":
                eventFileList =this.appendList(eventProperty.getDocFileList(), eventFileList);
                eventProperty.setDocFileList(eventFileList);
                break;
        }
        eventPropertyRepository.save(eventProperty);
    }

    private List<EventFile> appendList(List<EventFile> headList, List<EventFile> appendList){
        if(headList!=null){
            headList.addAll(appendList);
            return headList;
        }else
            return appendList;
    }

    /**
     * 删除单个资产文件
     * @param eventId
     * @param eventFileId
     * @param type
     */
    public void deleteEventPropertyFile(int eventId, int eventFileId, String type){
        EventProperty eventProperty =eventPropertyRepository.findById(eventId).get();
        EventFile eventFile =eventFileRepository.findById(eventFileId).get();
        fileService.deleteFileByPath(eventFile.getFileName(), eventFile.getFileId());
        switch (type){
            case "Picture":
                eventProperty.getPictureFileList().remove(eventFile);
                break;
            case "Video":
                eventProperty.getVideoFileList().remove(eventFile);
                break;
            case "Doc":
                eventProperty.getDocFileList().remove(eventFile);
                break;
        }
        eventFileRepository.delete(eventFile);
//        eventPropertyRepository.save(eventProperty);
    }

    public void downloadEventFile(int eventFileId, HttpServletResponse response){
        EventFile eventFile =eventFileRepository.findById(eventFileId).get();
        try{
            fileService.downloadFile(eventFile.getFileName(), eventFile.getFileId(),response);
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

    /**
     * 调用文件保存接口，保存文件列表
     * @param fileList
     * @param path
     * @param type
     * @return
     */
    private List<EventFile> saveFileList(List<MultipartFile> fileList, EventProperty eventProperty, String path, String type){
        if(fileList!=null && fileList.size()!=0){
            List<EventFile> eventFileList =new ArrayList<>();
            for(MultipartFile file:fileList){
                String fileId=fileService.addFileByPath(file,path);
                EventFile eventFile =new EventFile(file.getOriginalFilename(),fileId,type, eventProperty);
                eventFileList.add(eventFile);
            }
            eventFileRepository.saveBatch(eventFileList);
            return eventFileList;
        } else {
            log.info("寄");
          return null;
        }
    }

    /**
     * 删除资产
     * @param id
     */
    public void deleteEventProperty(int id){
        EventProperty eventProperty =eventPropertyRepository.findById(id).get();
        List<EventFile> eventFileList = eventProperty.getDocFileList();
        if(eventFileList !=null)this.simpleDeleteFileList(eventFileList);
        eventFileList = eventProperty.getVideoFileList();
        if(eventFileList !=null)this.simpleDeleteFileList(eventFileList);
        eventFileList = eventProperty.getPictureFileList();
        if(eventFileList !=null)this.simpleDeleteFileList(eventFileList);
        eventPropertyRepository.deleteById(id);
    }

    /**
     * 简单删除文件，不修改数据库
     * @param eventFileList
     */
    private void simpleDeleteFileList(List<EventFile> eventFileList){
        for(EventFile eventFile : eventFileList){
            fileService.deleteFileByPath(eventFile.getFileName(), eventFile.getFileId());
        }
    }
}

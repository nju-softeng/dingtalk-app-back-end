package com.softeng.dingtalk.service;

import com.aliyun.dingtalkdrive_1_0.models.GetDownloadInfoResponseBody;
import com.softeng.dingtalk.api.BaseApi;
import com.softeng.dingtalk.entity.ExternalPaper;
import com.softeng.dingtalk.entity.InternalPaper;
import com.softeng.dingtalk.entity.User;
import com.softeng.dingtalk.repository.ExternalPaperRepository;
import com.softeng.dingtalk.repository.InternalPaperRepository;
import com.softeng.dingtalk.vo.PaperFileDownloadInfoVO;
import com.softeng.dingtalk.vo.PaperFileInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * @author RickyWu
 * @description
 * @create 10/3/2022
 */
@Service
@Transactional
@Slf4j
public class PaperFileService {
    @Autowired
    BaseApi baseApi;
    @Autowired
    UserService userService;
    @Autowired
    FileService fileService;
    @Autowired
    PaperService paperService;
    @Autowired
    InternalPaperRepository internalPaperRepository;
    @Autowired
    ExternalPaperRepository externalPaperRepository;

    /**
     * 添加论文文件
     * @param paperId
     * @param uid
     * @param file
     * @param fileType
     */
    public void addPaperFile(int paperId, int uid, MultipartFile file, String fileType) {
        String fileId=fileService.addFile(file,uid);
        String fileName=file.getOriginalFilename();
        InternalPaper paper=paperService.getInternalPaper(paperId);
        switch (fileType){
            case "reviewFile":
                paper.setReviewFileId(fileId);
                paper.setReviewFileName(fileName);
                break;
            case "submissionFile":
                paper.setSubmissionFileId(fileId);
                paper.setSubmissionFileName(fileName);
                break;
            case "publishedFile":
                paper.setPublishedFileId(fileId);
                paper.setPublishedFileName(fileName);
                break;
            case "publishedLatexFile":
                paper.setPublishedLatexFileId(fileId);
                paper.setPublishedLatexFileName(fileName);
                break;
        }
        internalPaperRepository.save(paper);
    }

    /**
     * 添加外部评审论文文件
     * @param paperId
     * @param uid
     * @param file
     * @param fileType
     */
    public void addExternalPaperFile(int paperId, int uid, MultipartFile file, String fileType) {
        String fileId=fileService.addFile(file,uid);
        String fileName=file.getOriginalFilename();
        ExternalPaper paper=externalPaperRepository.findById(paperId).get();
        switch (fileType){
            case "reviewFile":
                paper.setReviewFileId(fileId);
                paper.setReviewFileName(fileName);
                break;
            case "submissionFile":
                paper.setSubmissionFileId(fileId);
                paper.setSubmissionFileName(fileName);
                break;
            case "publishedFile":
                paper.setPublishedFileId(fileId);
                paper.setPublishedFileName(fileName);
                break;
            case "publishedLatexFile":
                paper.setPublishedLatexFileId(fileId);
                paper.setPublishedLatexFileName(fileName);
                break;
        }
        externalPaperRepository.save(paper);
    }

    /**
     * 删除论文文件
     * @param uid
     * @param fileId
     * @param paperId
     * @param fileType
     */
    public void deletePaperFile(int uid, String fileId,int paperId,String fileType) {
        String unionId=userService.getUserUnionId(uid);
        InternalPaper paper=paperService.getInternalPaper(paperId);
        switch (fileType){
            case "reviewFile":
                paper.setReviewFileId(null);
                paper.setReviewFileName(null);
                break;
            case "submissionFile":
                paper.setSubmissionFileId(null);
                paper.setSubmissionFileName(null);
                break;
            case "publishedFile":
                paper.setPublishedFileId(null);
                paper.setPublishedFileName(null);
                break;
            case "publishedLatexFile":
                paper.setPublishedLatexFileId(null);
                paper.setPublishedLatexFileName(null);
                break;
        }
        baseApi.deleteFile(fileId,unionId);

    }

    /**
     * 获取论文下载信息
     * @param uid
     * @param fileId
     * @return
     */
    public PaperFileDownloadInfoVO getPaperFileDownloadInfo(int uid,String fileId){
        GetDownloadInfoResponseBody.GetDownloadInfoResponseBodyDownloadInfo fileDownloadInfo=baseApi.getFileDownloadInfo(userService.getUserUnionId(uid), fileId);
        PaperFileDownloadInfoVO paperFileDownloadInfoVO=new PaperFileDownloadInfoVO(fileDownloadInfo.getResourceUrl(),
                fileDownloadInfo.getHeaders().get("Authoration").toString(),fileDownloadInfo.getHeaders().get("Date").toString());
        return paperFileDownloadInfoVO;
    }

    /**
     * 获得论文文件信息
     * @param paperId
     * @return
     */
    public PaperFileInfoVO getPaperFileInfo(int paperId){
        return internalPaperRepository.getPaperFileInfo(paperId);
    }
}

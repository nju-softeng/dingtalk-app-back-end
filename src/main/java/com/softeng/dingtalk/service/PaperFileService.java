package com.softeng.dingtalk.service;

import com.aliyun.dingtalkdrive_1_0.models.GetDownloadInfoResponseBody;
import com.softeng.dingtalk.component.dingApi.BaseApi;
import com.softeng.dingtalk.entity.ExternalPaper;
import com.softeng.dingtalk.entity.InternalPaper;
import com.softeng.dingtalk.dao.repository.ExternalPaperRepository;
import com.softeng.dingtalk.dao.repository.InternalPaperRepository;
import com.softeng.dingtalk.vo.PaperFileDownloadInfoVO;
import com.softeng.dingtalk.vo.PaperFileInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
        InternalPaper paper=paperService.getInternalPaper(paperId);
        String fileId=fileService.addFileByPath(file,paper.getPath()+"/"+getFileTypeFolderName(fileType));
        String fileName=file.getOriginalFilename();
        setPaperFileIdAndName(paper,fileType,fileId,fileName);
        internalPaperRepository.save(paper);
        // 论文相关文件上传齐全后，触发论文作者加分
        if(paper.hasAccepted() && paper.hasCompleteFile()) {
            paperService.calculateInternalPaperAc(paper);
        }
    }

    /**
     * 添加外部评审论文文件
     * @param paperId
     * @param uid
     * @param file
     * @param fileType
     */
    public void addExternalPaperFile(int paperId, int uid, MultipartFile file, String fileType) {
        ExternalPaper paper=externalPaperRepository.findById(paperId).get();
        String fileId=fileService.addFileByPath(file,paper.getPath()+"/"+getFileTypeFolderName(fileType));
        String fileName=file.getOriginalFilename();
        setExternalPaperFileIdAndName(paper,fileType,fileId,fileName);
        externalPaperRepository.save(paper);
    }

    private String getFileTypeFolderName(String fileType){
        String res=fileType.substring(0,1).toUpperCase()+fileType.substring(1,fileType.length()-4);
        return res;
    }

    /**
     * 删除论文文件
     * @param fileName
     * @param fileId
     * @param paperId
     * @param fileType
     */
    public void deletePaperFile(String fileName, String fileId,int paperId,String fileType) {
//        String unionId=userService.getUserUnionId(uid);
        InternalPaper paper=paperService.getInternalPaper(paperId);
        setPaperFileIdAndName(paper,fileType,null,null);
        internalPaperRepository.save(paper);
        //钉盘删除方式
//        baseApi.deleteFile(fileId,unionId);
        fileService.deleteFileByPath(fileName,fileId);

    }

    /**
     * 删除外部评审论文文件
     * @param fileName
     * @param fileId
     * @param paperId
     * @param fileType
     */
    public void deleteExternalPaperFile(String fileName, String fileId,int paperId,String fileType) {
//        String unionId=userService.getUserUnionId(uid);
        ExternalPaper paper=externalPaperRepository.findById(paperId).get();
        setExternalPaperFileIdAndName(paper,fileType,null,null);
        externalPaperRepository.save(paper);
//        baseApi.deleteFile(fileId,unionId);
        fileService.deleteFileByPath(fileName,fileId);
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

    /**
     * 获得外部评审论文文件信息
     * @param paperId
     * @return
     */
    public PaperFileInfoVO getExternalPaperFileInfo(int paperId){
        return externalPaperRepository.getExternalPaperFileInfo(paperId);
    }

    private void setPaperFileIdAndName(InternalPaper paper, String fileType, String fileId, String fileName){
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
            case "publicFile":
                paper.setPublicFileId(fileId);
                paper.setPublicFileName(fileName);
                break;
            case "sourceFile":
                paper.setSourceFileId(fileId);
                paper.setSourceFileName(fileName);
                break;
            case "commentFile":
                paper.setCommentFileId(fileId);
                paper.setCommentFileName(fileName);
                break;
        }
    }
    private void setExternalPaperFileIdAndName(ExternalPaper paper, String fileType, String fileId, String fileName){
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
            case "publicFile":
                paper.setPublicFileId(fileId);
                paper.setPublicFileName(fileName);
                break;
            case "sourceFile":
                paper.setSourceFileId(fileId);
                paper.setSourceFileName(fileName);
                break;
            case "commentFile":
                paper.setCommentFileId(fileId);
                paper.setCommentFileName(fileName);
                break;
        }
    }
}

package com.softeng.dingtalk.service;

import com.softeng.dingtalk.api.BaseApi;
import com.softeng.dingtalk.encryption.Encryption;
import com.softeng.dingtalk.entity.*;
import com.softeng.dingtalk.repository.*;
import com.softeng.dingtalk.vo.UserInfoVO;
import com.softeng.dingtalk.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * @author zhanyeye
 * @description User业务逻辑组件
 * @date 12/7/2019
 */
@Service
@Transactional
@Slf4j
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AcRecordRepository acRecordRepository;
    @Autowired
    private FileService fileService;
    @Autowired
    BaseApi baseApi;
    @Autowired
    Encryption encryption;

    /**
     * @author LiXiaoKang
     * @description 新增用用户权限、用户组相关注入
     * @date 2/5/2023
     */
    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private UserPermissionRepository userPermissionRepository;
    @Autowired
    private UserTeamRepository userTeamRepository;

    @Value("${file.userLeaseContractFilePath}")
    private String userLeaseContractFilePath;
    /**
     * 判断用户权限是否为审核人
     * @param uid 用户id
     * @return
     */
    public boolean isAuditor(int uid) {
        return userRepository.getUserAuthority(uid) == User.AUDITOR_AUTHORITY;
    }


    /**
     * 查询系统可用用户
     * @return
     */
    public List<UserVO> listUserVO() {
        return userRepository.listUserVOS();
    }


    /**
     * 获取用户的userID （获取周报）
     * @param id
     * @return
     */
    public String getUserid(int id) {
        return userRepository.findById(id).get().getUserid();
    }

    public String getUserUnionId(int uid) {return userRepository.findById(uid).get().getUnionid();}
    /**
     * 通过useID获取用户 -> 通过用户进入系统时调用API获得的userID查询用户，判断用户是否在系统中，还是新用户
     * @param userid
     * @return
     */
    public User getUser(String userid) {
        return userRepository.findByUserid(userid);
    }


    /**
     * 查询所有的审核员 -> 供用户提交审核申请时选择
     * @return
     */
    public Map getAuditorUser() {
        return Map.of("auditorlist", userRepository.listAuditor());
    }


    /**
     * 获取用户信息
     * @param uid
     * @return
     */
    public Map getUserInfo(int uid) {
        User u = userRepository.findById(uid).get();
        double ac = acRecordRepository.getUserAcSum(uid);
        baseApi.getJsapiTicket(); // 提前拿到jsapi ticket，避免需要时再去那减少时延
        if (u.getAvatar() != null) {
            return Map.of("name", u.getName(), "avatar", u.getAvatar(), "ac", ac, "userid", u.getUserid());
        } else {
            return Map.of("name", u.getName(), "ac", ac, "userid", u.getUserid());
        }
    }


    /**
     * 更新用户权限
     * @param uid
     * @param authority
     */
    public void updateRole(int uid, int authority) {
        userRepository.updateUserRole(uid, authority);
    }


    /**
     * 查询用户详情
     * @param uid
     * @return
     */
    public UserInfoVO getUserDetail(int uid) {
        User u = userRepository.findById(uid).get();
        return new UserInfoVO(u.getName(), u.getAvatar(), u.getPosition(), u.getStuNum(), u.getUndergraduateCollege(), u.getMasterCollege(), encryption.doDecrypt(u.getIdCardNo()), encryption.doDecrypt(u.getCreditCard()), u.getBankName(),u.getRentingStart(), u.getRentingEnd(), u.getAddress(), u.getWorkState(), u.getRemark(),u.getLeaseContractFileName(),u.getLeaseContractFilePath());
    }


    public void updateUserInfo(UserInfoVO userInfoVO, int uid) {
        User u = userRepository.findById(uid).get();
        u.setStuNum(userInfoVO.getStuNum());
        u.setName(userInfoVO.getName());
        u.setCreditCard(encryption.doEncrypt(userInfoVO.getCreditCard()));
        u.setIdCardNo(encryption.doEncrypt(userInfoVO.getIdCardNo()));
        u.setBankName(userInfoVO.getBankName());
        u.setMasterCollege(userInfoVO.getMasterCollege());
        u.setUndergraduateCollege(userInfoVO.getUndergraduateCollege());
        u.setWorkState(userInfoVO.getWorkState());
        u.setRemark(userInfoVO.getRemark());
        u.setAddress(userInfoVO.getAddress());
        u.setRentingEnd(userInfoVO.getRentingEnd());
        u.setRentingStart(userInfoVO.getRentingStart());
        userRepository.save(u);
    }

    public void saveLeaseContractFile(MultipartFile file, int uid){
       User user=userRepository.findById(uid).get();
       user.setLeaseContractFileName(file.getOriginalFilename());
       user.setLeaseContractFilePath(fileService.addFileByPath(file,userLeaseContractFilePath+user.getStuNum()));
       userRepository.save(user);
    }

    public void downloadContractFile(int uid, HttpServletResponse httpServletResponse) throws IOException {
        User user=userRepository.findById(uid).get();
        String fileName=user.getLeaseContractFileName();
        String filePath=user.getLeaseContractFilePath();
        fileService.downloadFile(fileName,filePath,httpServletResponse);
    }

    /**
     * @author LiXiaoKang
     * @description 新增用用户权限、用户组相关service层方法
     * @date 2/5/2023
     */

    /**
     * 获得该用户的所有权限名
     * @param userId
     * @return 所有权限名
     */
    public List<String> getPermissionNames(int userId){
        List<String> permissionList= new ArrayList<>();
        List<UserPermission> userPermissionList= userPermissionRepository.findAllByUserId(userId);
        for (UserPermission userPermission: userPermissionList) {
            Permission permission = permissionRepository.findById(userPermission.getPermissionId());
            permissionList.add(permission.getName());
        }
        return permissionList;
    }

    /**
     * 获得该用户所在的所有用户组名
     * @param userId
     * @return
     */
    public List<String> getTeams(int userId){
        List<String> teamList= new ArrayList<>();
        List<UserTeam> userTeamList= userTeamRepository.findAllByUserId(userId);
        for (UserTeam userTeam: userTeamList) {
            Team team = teamRepository.findById(userTeam.getTeamId());
            teamList.add(team.getName());
        }
        return teamList;
    }
}

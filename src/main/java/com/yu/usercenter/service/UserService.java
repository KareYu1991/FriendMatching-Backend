package com.yu.usercenter.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yu.usercenter.model.domain.Tag;
import com.yu.usercenter.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yu.usercenter.model.domain.request.TagSearch;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户服务
 */
public interface UserService extends IService<User> {


    /**
     * 分页查询
     */
    Page<User> selectUserPages(Long pageSize, Long pageNum, HttpServletRequest request);

    /**
     * 根据标签查询用户
     */
    List<User> searchUserListByTags(TagSearch tagSearch);

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param planetCode    星球编号
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    User getSafetyUser(User originUser);


    /**
     * 用户注销
     */
    int userLogout(HttpServletRequest request);

    /**
     * 更新用户
     */
    User updateUser(User user, User loginUser);

    /**
     * 查询当前登录用户
     */
    User getLoginUserInfo(HttpServletRequest request);

    boolean isAdmin(User user);

    List<User> matchUsers(long num, User user);
}

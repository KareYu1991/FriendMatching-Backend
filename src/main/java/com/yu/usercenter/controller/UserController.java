package com.yu.usercenter.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yu.usercenter.contant.UserConstant;
import com.yu.usercenter.exception.BusinessException;
import com.yu.usercenter.model.domain.request.TagSearch;
import com.yu.usercenter.service.UserService;
import com.yu.usercenter.common.BaseResponse;
import com.yu.usercenter.common.ErrorCode;
import com.yu.usercenter.common.ResultUtils;
import com.yu.usercenter.model.domain.User;
import com.yu.usercenter.model.domain.request.UserLoginRequest;
import com.yu.usercenter.model.domain.request.UserRegisterRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户接口
 */
@Api(tags = "用户控制")
@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 获取最匹配的用户
     *
     * @param num
     * @param request
     * @return
     */
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest request) {
        if (num <= 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUserInfo(request);
        return ResultUtils.success(userService.matchUsers(num, user));
    }

    @GetMapping("/recommend")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageSize", value = "条数", required = true, type = "long"),
            @ApiImplicitParam(name = "pageNum", value = "页码", required = true, type = "long"),
    })
    public BaseResponse<Page<User>> recommendUsers(Long pageSize, Long pageNum, HttpServletRequest request) {
        Page<User> userPage = userService.selectUserPages(pageSize, pageNum, request);
        return ResultUtils.success(userPage);
    }


    @PostMapping("/update")
    public BaseResponse<User> updateUser(@RequestBody User user, HttpServletRequest request) {
        if (Objects.isNull(user) || Objects.isNull(user.getId())) {
            return ResultUtils.error(ErrorCode.NULL_ERROR);
        }
        User loginUser = userService.getLoginUserInfo(request);
        User updateUser = userService.updateUser(user, loginUser);
        return ResultUtils.success(updateUser);
    }


    @GetMapping("/search/tags")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "tagNames", value = "标签列表",
                    required = false, type = "List"),
    })
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam List<String> tagNames) {
        if (CollectionUtil.isEmpty(tagNames)) {
            return ResultUtils.success(CollectionUtil.newArrayList());
        }
        TagSearch tagSearch = new TagSearch();
        tagSearch.setTagNames(tagNames);
        List<User> userList = userService.searchUserListByTags(tagSearch);
        return ResultUtils.success(userList);
    }

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    @ApiOperation(value = "用户注册", notes = "用户注册")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 校验
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            return null;
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前用户
     *
     * @param request
     * @return
     */
    @GetMapping("/current")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "request", value = "请求对象",
                    required = false, type = "HttpServletRequest"),
    })
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long userId = currentUser.getId();
        // TODO 校验用户是否合法
        User user = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }


    @GetMapping("/search")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", value = "用户名",
                    required = false, type = "String"),
            @ApiImplicitParam(name = "request", value = "请求对象",
                    required = false, type = "HttpServletRequest"),
    })
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "缺少管理员权限");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(id);
        return ResultUtils.success(b);
    }


    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    private boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == UserConstant.ADMIN_ROLE;
    }

}

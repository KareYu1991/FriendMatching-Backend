package com.yu.usercenter.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yu.usercenter.contant.UserConstant;
import com.yu.usercenter.mapper.UserMapper;
import com.yu.usercenter.model.domain.request.TagSearch;
import com.yu.usercenter.service.UserService;
import com.yu.usercenter.common.ErrorCode;
import com.yu.usercenter.exception.BusinessException;
import com.yu.usercenter.model.domain.User;
import com.yu.usercenter.utils.algorithm.AlgorithmUtils;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "yu";

    /**
     * 获取相似匹配用户
     */
    @Override
    public List<User> matchUsers(long num, User loginUser) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "tags");
        queryWrapper.isNotNull("tags");
        queryWrapper.ne("tags","[]");
        List<User> userList = this.list(queryWrapper);
        String tags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        // 用户列表的下标 => 相似度
        List<Pair<User, Long>> list = new ArrayList<>();
        // 依次计算所有用户和当前用户的相似度
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            String userTags = user.getTags();
            // 无标签或者为当前用户自己
            if (StringUtils.isBlank(userTags) || user.getId().equals(loginUser.getId())) {
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            // 计算分数
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            list.add(new Pair<>(user, distance));
        }
        // 按编辑距离由小到大排序
        List<Pair<User, Long>> topUserPairList = list.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        // 原本顺序的 userId 列表
        List<Long> userIdList = topUserPairList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());
//        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
//        userQueryWrapper.in("id", userIdList);

        // 1, 3, 2
        // User1、User2、User3
        // 1 => User1, 2 => User2, 3 => User3
        Map<Long, List<User>> userIdUserListMap = userMapper.selectBatchIds(userIdList)
                .stream()
                .map(this::getSafetyUser)
                .collect(Collectors.groupingBy(User::getId));
        List<User> finalUserList = new ArrayList<>();
        for (Long userId : userIdList) {
            finalUserList.add(userIdUserListMap.get(userId).get(0));
        }
        return finalUserList;
    }


    @Override
    public Page<User> selectUserPages(Long pageSize, Long pageNum, HttpServletRequest request) {
        User loginUserInfo = getLoginUserInfo(request);
        String redisKey = getRedisKey(loginUserInfo.getId().toString());
        ValueOperations valueOperations = redisTemplate.opsForValue();
        Page<User> cachePage = (Page<User>) valueOperations.get(redisKey);
        if (Objects.nonNull(cachePage)) {
            return cachePage;
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<User>();
        Page<User> userPage = userMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        if (Objects.nonNull(userPage)) {
            try {
                valueOperations.set(redisKey, userPage, 30, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.debug("生成缓存失败");
            }
        }
        return userPage;
    }

    private String getRedisKey(String id) {
        return String.format("partner:user:recommend:%s", id);
    }

    /**
     * 更新用户数据
     */
    @Override
    public User updateUser(User user, User loginUser) {
        if (Objects.isNull(loginUser)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        boolean admin = isAdmin(loginUser);
        if (!admin) {
            // 不是管理员只能修改自己
            if (loginUser.getId().equals(user.getId())) {
                userMapper.updateById(user);
            } else {
                throw new BusinessException(ErrorCode.NO_AUTH, "您只能修改自己");
            }
        } else {
            userMapper.updateById(user);
        }
        return this.getById(user.getId());
    }

    /**
     * 是否为管理员
     */
    @Override
    public boolean isAdmin(User user) {
        return user != null && user.getUserRole() == UserConstant.ADMIN_ROLE;
    }


    @Override
    public User getLoginUserInfo(HttpServletRequest request) {
        User user = null;
        if (Objects.isNull(request) || Objects.isNull(request.getSession())) {
            return user;
        }
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        user = (User) userObj;
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return user;
    }


    /**
     * 根据标签查询用户
     * <p>
     * 两种方式：
     * 1是直接sql查询并返回
     * 2是先查询所有，再在内存中做筛选
     */
    @Override
    public List<User> searchUserListByTags(TagSearch tagSearch) {
        List<User> result = CollectionUtil.newArrayList();
        List<String> tagNames = tagSearch.getTagNames();
        if (CollectionUtil.isEmpty(tagNames)) {
            return result;
        }
        // 第一种方法 直接sql查询并返回
//        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        for (String tagName : tagNames) {
//            queryWrapper = queryWrapper.like("tags",tagName);
//        }
//        List<User> users = userMapper.selectList(queryWrapper);
//        if (CollectionUtil.isNotEmpty(users)) {
//            result = users.stream().map(this::getSafetyUser).collect(Collectors.toList());
//        }

        // 第二种方法 先查询所有，再在内存中做筛选】
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> users = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();
        if (CollectionUtil.isEmpty(users)) {
            return result;
        }
        result = users.stream().filter(u -> {
            String tags = u.getTags();
            if (StrUtil.isBlankIfStr(tags)) {
                return false;
            }
            Set<String> uTagNameSet = gson.fromJson(tags, new TypeToken<Set<String>>() {
            }.getType());
            uTagNameSet = Optional.ofNullable(uTagNameSet).orElse(new HashSet<>());
            // 怎么比才合适 只要有符合的标签就返回，还是只要全符合的才返回
            for (String tagName : tagNames) {
                if (uTagNameSet.contains(tagName)) {
                    log.debug("包含标签：" + tagName);
                    return true;
                }
            }
            return false;
        }).map(this::getSafetyUser).collect(Collectors.toList());
        return result;
    }

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param planetCode    星球编号
     * @return 新用户 id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (planetCode.length() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长");
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return -1;
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            return -1;
        }
        // 账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 星球编号不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编号重复");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 3. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            return -1;
        }
        return user.getId();
    }


    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return null;
        }
        if (userAccount.length() < 4) {
            return null;
        }
        if (userPassword.length() < 8) {
            return null;
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return null;
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            return null;
        }
        // 3. 用户脱敏
        User safetyUser = getSafetyUser(user);
        // 4. 记录用户的登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, safetyUser);
        return safetyUser;
    }

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setTags(originUser.getTags());
        safetyUser.setProfile(originUser.getProfile());
        return safetyUser;
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // 移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return 1;
    }

}

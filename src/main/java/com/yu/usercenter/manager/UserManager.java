package com.yu.usercenter.manager;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yu.usercenter.mapper.UserMapper;
import com.yu.usercenter.model.domain.User;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
public class UserManager {

    @Resource
    private UserMapper userMapper;

    public int insert(User user) {
        return userMapper.insert(user);
    }

    public List<User> selectBatchIds(List<Long> userIds) {
        return userMapper.selectBatchIds(userIds);
    }
}

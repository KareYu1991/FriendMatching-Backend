package com.yu.usercenter.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yu.usercenter.mapper.UserTeamMapper;
import com.yu.usercenter.model.domain.UserTeam;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Component
public class UserTeamManager {

    @Resource
    private UserTeamMapper userTeamMapper;

    public int insert(UserTeam userTeam) {
        return userTeamMapper.insert(userTeam);
    }

    public List<UserTeam> selectUserByTeamId(Long teamId, Long userId) {
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        if (Objects.nonNull(userId)) {
            queryWrapper.eq(UserTeam.COL_USERID, userId);
        }
        if (Objects.nonNull(teamId)) {
            queryWrapper.eq(UserTeam.COL_TEAMID, teamId);
        }
        return userTeamMapper.selectList(queryWrapper);
    }

    public long count(Long userId, Long teamId) {
        return selectUserByTeamId(teamId,userId).size();
    }

    public int deleteByUserIdOrTeamId(Long teamId, Long userId) {
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        if (Objects.nonNull(userId)) {
            queryWrapper.eq(UserTeam.COL_USERID, userId);
        }
        if (Objects.nonNull(teamId)) {
            queryWrapper.eq(UserTeam.COL_TEAMID, teamId);
        }
        return userTeamMapper.delete(queryWrapper);
    }

    public List<UserTeam> selectUserByTeamIds(List<Long> teamIds) {
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        if (CollectionUtil.isNotEmpty(teamIds)) {
            queryWrapper.in(UserTeam.COL_TEAMID, teamIds);
        }
        return userTeamMapper.selectList(queryWrapper);
    }
}

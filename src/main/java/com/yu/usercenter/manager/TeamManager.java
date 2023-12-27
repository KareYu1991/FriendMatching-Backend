package com.yu.usercenter.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yu.usercenter.common.enums.TeamStatusEnum;
import com.yu.usercenter.mapper.TeamMapper;
import com.yu.usercenter.model.domain.Team;
import com.yu.usercenter.model.dto.TeamQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class TeamManager {

    @Resource
    private TeamMapper teamMapper;

    public Team selectById(Long id) {
        return teamMapper.selectById(id);
    }

    public List<Team> selectByIds(List<Long> ids) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<Team>();
        if (CollectionUtil.isNotEmpty(ids)) {
            queryWrapper.in(Team.COL_ID, ids);
        }
        return teamMapper.selectList(queryWrapper);
    }

    public Page<Team> queryTeamPage(TeamQuery teamQuery,List<Long> ids) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<Team>();
        queryWrapper.in(Team.COL_ID,ids);
        return teamMapper.selectPage(new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize()), queryWrapper);
    }

    public Page<Team> queryTeamPage(TeamQuery teamQuery, Boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        //组合查询条件
        if (teamQuery != null) {
            // 查询自己
            if (!teamQuery.getAllSee().equals(0)) {
                return queryTeamPage(teamQuery);
            }

            // 队伍id
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            //
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
//                queryWrapper.and(qw -> qw.like("name", searchText).or().like("expireTime", searchText));
                queryWrapper.and(qw -> qw.like("name", searchText));
            }
            // 名称筛选
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            // 描述筛选
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            // 人数筛选
            Integer maxNum = teamQuery.getMaxNum();
            //查询最大人数相等
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxMum", maxNum);
            }

            //根据状态来查询
            Integer status = teamQuery.getStatus();
//            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(3);
            // 默认可以查看公开和加密房
            List<Integer> statusValues = CollectionUtil.newArrayList(TeamStatusEnum.PUBLIC.getValue(), TeamStatusEnum.SECRET.getValue());
            if (status != null) {
                queryWrapper.eq(Team.COL_STATUS, status);
                // 管理员可查询私密房，其他人不行
//                if (TeamStatusEnum.PRIVATE.getValue() == status && isAdmin) {
//                    queryWrapper.eq(Team.COL_STATUS, status);
//                } else {
//                    queryWrapper.eq(Team.COL_STATUS, statusValues);
//                }
            } else {
                if (!isAdmin) {
                    queryWrapper.in(Team.COL_STATUS, statusValues);
                }
            }
            // 查询创建人
            Long userId = teamQuery.getUserId();
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }
        }
        // 筛选掉过期的房间
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        return teamMapper.selectPage(new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize()), queryWrapper);
    }

    public Page<Team> queryTeamPage(TeamQuery teamQuery) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        //组合查询条件
        if (teamQuery != null) {
            // 队伍id
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            //
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
//                queryWrapper.and(qw -> qw.like("name", searchText).or().like("expireTime", searchText));
                queryWrapper.and(qw -> qw.like("name", searchText));
            }
            // 名称筛选
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            // 描述筛选
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            // 人数筛选
            Integer maxNum = teamQuery.getMaxNum();
            //查询最大人数相等
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxMum", maxNum);
            }

            //根据状态来查询
            Integer status = teamQuery.getStatus();
            if (status != null) {
                queryWrapper.eq(Team.COL_STATUS, status);
            }
            // 查询创建人
            Long userId = teamQuery.getUserId();
            if (userId != null && userId > 0) {
                queryWrapper.eq(Team.COL_USERID, userId);
            }
        }
        return teamMapper.selectPage(new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize()), queryWrapper);
    }

    public int updateTeam(Team newTeam) {
        return teamMapper.updateById(newTeam);
    }

    public int deleteTeam(Long teamId) {
        return teamMapper.deleteById(teamId);
    }
}

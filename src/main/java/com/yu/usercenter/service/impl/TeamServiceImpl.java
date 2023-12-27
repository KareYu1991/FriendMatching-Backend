package com.yu.usercenter.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yu.usercenter.common.ErrorCode;
import com.yu.usercenter.common.enums.TeamStatusEnum;
import com.yu.usercenter.contant.UserConstant;
import com.yu.usercenter.exception.BusinessException;
import com.yu.usercenter.manager.TeamManager;
import com.yu.usercenter.manager.UserManager;
import com.yu.usercenter.manager.UserTeamManager;
import com.yu.usercenter.mapper.UserMapper;
import com.yu.usercenter.mapper.UserTeamMapper;
import com.yu.usercenter.model.domain.User;
import com.yu.usercenter.model.domain.UserTeam;
import com.yu.usercenter.model.domain.request.TeamAddRequest;
import com.yu.usercenter.model.domain.request.TeamJoinRequest;
import com.yu.usercenter.model.domain.request.TeamQuitRequest;
import com.yu.usercenter.model.domain.request.TeamUpdateRequest;
import com.yu.usercenter.model.dto.TeamQuery;
import com.yu.usercenter.model.vo.TeamUserVO;
import com.yu.usercenter.model.vo.UserVO;
import com.yu.usercenter.service.UserService;
import com.yu.usercenter.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yu.usercenter.mapper.TeamMapper;
import com.yu.usercenter.model.domain.Team;
import com.yu.usercenter.service.TeamService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {

    @Resource
    private TeamManager teamManager;

    @Resource
    private UserTeamManager userTeamManager;

    @Resource
    private UserManager userManager;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 查询自己加入的队伍
     */
    @Override
    public Page<TeamUserVO> listTeamsByPageJoin(TeamQuery teamQuery, Long userId) {
        Page<TeamUserVO> page = new Page<>();
        List<TeamUserVO> teamUserVOS = new ArrayList<>();
        page.setRecords(teamUserVOS);
        List<UserTeam> userTeams = userTeamManager.selectUserByTeamId(null, userId);
        List<Long> teamIds = userTeams.stream().map(UserTeam::getTeamId).collect(Collectors.toList());
        Page<Team> teamPage = teamManager.queryTeamPage(teamQuery, teamIds);
        getTeamUserVOPage(page, teamUserVOS, teamPage, userId);
        return page;
    }


    /**
     * 创建队伍
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Team createTeam(Team team, User loginUser) {
        Long userId = loginUser.getId();
        validatedTeam(team, userId, true);
        //8.插入队伍消息到队伍表
        team.setUserId(userId);
        boolean save = this.save(team);
        Long id = team.getId();
        if (!save || Objects.isNull(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        // 维护关联表
        UserTeam userTeam = new UserTeam();
        userTeam.setTeamId(id);
        userTeam.setUserId(userId);
        userTeam.setJoinTime(new Date());
        int insert = userTeamManager.insert(userTeam);
        if (insert <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        return this.baseMapper.selectById(id);
    }

    @Override
    public List<Team> selectTeamsByList(List<Long> ids) {
        return teamManager.selectByIds(ids);
    }

    /**
     * 首先组合查询条件
     */
    @Override
    public Page<TeamUserVO> listTeamsByPage(TeamQuery teamQuery, User loginUser) {
        Page<TeamUserVO> page = new Page<>();
        List<TeamUserVO> teamUserVOS = new ArrayList<>();
        // 组合条件查询
        boolean isAdmin = loginUser.getUserRole() == UserConstant.ADMIN_ROLE;
        Page<Team> teamPage = teamManager.queryTeamPage(teamQuery, isAdmin);
        getTeamUserVOPage(page, teamUserVOS, teamPage, loginUser.getId());
        return page;
    }

    private Page<TeamUserVO> getTeamUserVOPage(Page<TeamUserVO> page, List<TeamUserVO> teamUserVOS, Page<Team> teamPage, Long loginUserId) {
        List<Team> records = teamPage.getRecords();
        if (CollectionUtil.isEmpty(records)) {
            return page;
        }
        // 填充队长信息
        List<Long> userIds = records.stream().map(Team::getUserId).collect(Collectors.toList());
        List<User> userList = userManager.selectBatchIds(userIds);
        if (CollectionUtil.isEmpty(userList)) {
            return page;
        }
        Map<Long, User> userMap = userList.stream().collect(Collectors.toMap(User::getId, v -> v));

        // 队伍的所有队员
        List<Long> teamIds = records.stream().map(Team::getId).collect(Collectors.toList());
        List<UserTeam> userTeams = userTeamManager.selectUserByTeamIds(teamIds);
        Map<Long, List<UserTeam>> userTeamsMap = userTeams.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));

        for (Team team : records) {
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            Long userId = team.getUserId();
            User user = userMap.get(userId);
            if (Objects.nonNull(user)) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            Long teamId = team.getId();
            List<UserTeam> userTeams1 = userTeamsMap.get(teamId);
            if (CollectionUtil.isNotEmpty(userTeams1)) {
                long has = userTeams1.stream().filter(s -> s.getUserId().equals(loginUserId)).count();
                if (has > 0 | userId.equals(loginUserId)) {
                    teamUserVO.setHasJoin(true);
                }
                teamUserVO.setHasJoinNum(userTeams1.size());
            } else {
                teamUserVO.setHasJoinNum(1);
            }
            teamUserVOS.add(teamUserVO);
        }
        teamPage.setRecords(null);
        BeanUtils.copyProperties(teamPage, page);
        page.setRecords(teamUserVOS);
        return page;
    }

    /**
     * 更新队伍
     * 校验
     */
    @Override
    public boolean updateTeam(TeamUpdateRequest updateRequest, User loginUser) {
        Long userId = loginUser.getId();
        Team team = new Team();
        Long id = updateRequest.getId();
        BeanUtils.copyProperties(updateRequest, team);
        Team oldTeam = this.getById(id);
        // 如果不是当前用户并且也不是管理员则不能修改
        if (!oldTeam.getUserId().equals(userId) && !(loginUser.getUserRole() == UserConstant.ADMIN_ROLE)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        boolean flag = validatedTeam(team, userId, false);
        if (flag) {
            return this.updateById(team);
        }
        return false;
    }

    /**
     * 加入队伍(直接加入接口)
     * 队伍是否有空位
     * 队伍有密码验证密码
     * -----
     * 加入队伍？直接加入 和  申请加入(通知队长是否允许加入)
     */
    @Override
    public boolean joinTeam(TeamJoinRequest joinRequest, User loginUser) {
        if (joinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = joinRequest.getTeamId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }
        String password = joinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }
        //该用户已加入的队伍数量 数据库查询所以放到下面，减少查询时间
        Long userId = loginUser.getId();
        RLock lock = redissonClient.getLock("yu:join_team");
        try {
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    long hasJoinNum = userTeamManager.count(userId, null);
                    if (hasJoinNum > 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建和加入5个队伍");
                    }
                    //不能重复加入已加入的队伍
                    long hasUserJoinTeam = userTeamManager.count(userId, teamId);
                    if (hasUserJoinTeam > 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该队伍");
                    }
                    //已加入队伍的人数
                    long teamHasJoinNum = userTeamManager.count(null, teamId);
                    if (teamHasJoinNum >= team.getMaxNum()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
                    }
                    //修改队伍信息
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamManager.insert(userTeam) > 0;
                }
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 退出队伍
     * 队伍是否存在
     * 队伍是否有你
     * 队伍退出后
     * 如果只剩自己，解散队伍
     * 如果还有人，如果是队长，移交队长给最早进入的人
     * 不是则自己退出
     * <p>
     * -----
     * 通知所有人？--广播刷新列表
     * 可以记录自己的退出时间？增加记录表？
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean quitTeam(TeamQuitRequest quitRequest, User loginUser) {
        // 应该前端穿用户id
        Long userId = loginUser.getId();
        Long teamId = quitRequest.getTeamId();
        Team team = this.teamManager.selectById(teamId);
        if (Objects.isNull(team)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        // 查询所有队员
        List<UserTeam> userTeams = this.userTeamManager.selectUserByTeamId(teamId, null);
        if (CollectionUtil.isEmpty(userTeams)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您不在此队伍中");
        }
        Map<Long, UserTeam> userTeamMap = userTeams.stream().collect(Collectors.toMap(UserTeam::getUserId, v -> v));
        if (Objects.isNull(userTeamMap.get(userId))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您不在此队伍中");
        }
        // 队伍只剩自己，解散队伍
        if (userTeamMap.keySet().size() == 1 && userTeamMap.get(userId).getUserId().equals(userId)) {
            removeTeam(teamId);
        }
        // 还有人 如果自己是队长，移交队长
        Long leaderId = team.getUserId();
        if (leaderId.equals(userId) && userTeams.size() > 1) {
            List<UserTeam> sortList = userTeams.stream()
                    .filter(s -> s.getUserId().equals(userId))
                    .sorted(Comparator.comparingLong(o -> o.getJoinTime().getTime()))
                    .collect(Collectors.toList());
            if (CollectionUtil.isNotEmpty(sortList)) {
                UserTeam userTeam = sortList.get(0);
                Long newLeaderId = userTeam.getUserId();
                Team newTeam = new Team();
                newTeam.setId(teamId);
                newTeam.setUserId(newLeaderId);
                int i = teamManager.updateTeam(newTeam);
                if (i <= 0) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "移交队长失败");
                }
            }
        }
        // 退出队伍
        int i = userTeamManager.deleteByUserIdOrTeamId(teamId, userId);
        if (i <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "退出失败");
        }
        return true;
    }

    /**
     * 是否为队长，队长能解散队伍
     * 是否为管理员，管理员能解散队伍
     * 删除队伍，删除关系表
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteTeam(TeamQuitRequest quitRequest, User loginUser) {
        Long userId = loginUser.getId();
        Long teamId = quitRequest.getTeamId();
        Team team = this.teamManager.selectById(teamId);
        if (Objects.isNull(team)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        Long leaderId = team.getUserId();
        boolean isAdmin = UserConstant.ADMIN_ROLE == loginUser.getUserRole();
        // 不是队长并且不是管理员，不能解散队伍
        if (!leaderId.equals(userId) && !isAdmin) {
            throw new BusinessException(ErrorCode.NO_AUTH, "您不能解散队伍");
        }
        removeTeam(teamId);
        return true;
    }


    private void removeTeam(Long teamId) {
        int i = teamManager.deleteTeam(teamId);
        if (i <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "解散队伍失败");
        }
        int i1 = userTeamManager.deleteByUserIdOrTeamId(teamId, null);
        if (i1 <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "解散队伍失败");
        }
    }


    private boolean validatedTeam(Team team, Long userId, boolean ua) {
        //(1).队伍人数<1且<=20
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);//如果为空，直接赋值为0
        if (ua && (maxNum < 1 || maxNum > 20)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不满足要求");
        }
        //(2).队伍标题 <=20
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题不满足要求");
        }
        // 3. 描述<= 512
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述过长");
        }
        //4.status 是否公开，不传默认为0
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不满足要求");
        }

        //5.如果status是加密状态，一定要密码 且密码<=32
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码设置不正确");
            }
        }
        //6.超出时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if (Objects.isNull(expireTime)) {
            long time = new Date().getTime();
            // 默认设置5分钟后过期
            expireTime = new Date(time + 300000);
            team.setExpireTime(expireTime);
        } else {
            if (new Date().after(expireTime)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "超出时间 > 当前时间");
            }
        }

        //7.校验用户最多创建5个队伍
        //todo 有bug。可能同时创建100个队伍
        if (ua) {
            QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userId", userId);
            long hasTeamNum = this.count(queryWrapper);
            if (hasTeamNum >= 5) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建5个队伍");
            }
        }
        return true;
    }
}

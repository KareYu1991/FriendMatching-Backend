package com.yu.usercenter.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yu.usercenter.common.BaseResponse;
import com.yu.usercenter.common.ErrorCode;
import com.yu.usercenter.common.ResultUtils;
import com.yu.usercenter.contant.UserConstant;
import com.yu.usercenter.exception.BusinessException;
import com.yu.usercenter.model.domain.Team;
import com.yu.usercenter.model.domain.User;
import com.yu.usercenter.model.domain.request.*;
import com.yu.usercenter.model.dto.TeamQuery;
import com.yu.usercenter.model.vo.TeamUserVO;
import com.yu.usercenter.service.TeamService;
import com.yu.usercenter.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Api(tags = "队伍控制")
@RestController
@RequestMapping("/team")
@CrossOrigin
public class TeamController {

    @Resource
    private TeamService teamService;

    @Resource
    private UserService userService;
    @GetMapping("/list/page/join")
    @ApiOperation("加入队伍分页条件查询")
    public BaseResponse<Page<TeamUserVO>> listTeamsByPageJoin(TeamQuery teamQuery, HttpServletRequest request) {
        User loginUser = userService.getLoginUserInfo(request);
        if (Objects.isNull(teamQuery) || Objects.isNull(loginUser)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<TeamUserVO> resultPage = teamService.listTeamsByPageJoin(teamQuery,loginUser.getId());
        return ResultUtils.success(resultPage);
    }


    @GetMapping("/list")
    @ApiOperation("分页条件查询")
    public BaseResponse<List<Team>> listTeams(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<Team> teamList = teamService.selectTeamsByList(null);
        return ResultUtils.success(teamList);
    }

    @GetMapping("/list/page")
    @ApiOperation("分页条件查询")
    public BaseResponse<Page<TeamUserVO>> listTeamsByPage(TeamQuery teamQuery, HttpServletRequest request) {
        User loginUser = userService.getLoginUserInfo(request);
        if (Objects.isNull(teamQuery) || Objects.isNull(loginUser)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean admin = userService.isAdmin(loginUser);
        Page<TeamUserVO> resultPage = teamService.listTeamsByPage(teamQuery, loginUser);
        return ResultUtils.success(resultPage);
    }


    @PostMapping("/add")
    @ApiOperation("创建队伍")
    public BaseResponse<Team> createTeam(@RequestBody TeamAddRequest addRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUserInfo(request);
        if (Objects.isNull(addRequest) || Objects.isNull(loginUser)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(addRequest, team);
        Team newTeam = teamService.createTeam(team, loginUser);
        return ResultUtils.success(newTeam);
    }

    @PostMapping("/update")
    @ApiOperation("修改队伍")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest updateRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUserInfo(request);
        if (Objects.isNull(updateRequest) || Objects.isNull(loginUser)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = teamService.updateTeam(updateRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    @ApiOperation("根据id查询队伍")
    @ApiImplicitParams({@ApiImplicitParam(name = "id", value = "队伍id", required = true, type = "long")})
    public BaseResponse<Team> getTeamById(@RequestParam("id") long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(team);
    }

    @PostMapping("/join")
    @ApiOperation("加入队伍")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamJoinRequest joinRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUserInfo(request);
        if (Objects.isNull(joinRequest) || Objects.isNull(loginUser)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = teamService.joinTeam(joinRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加入失败");
        }
        return ResultUtils.success(true);
    }

    @PostMapping("/quit")
    @ApiOperation("退出队伍")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest quitRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUserInfo(request);
        if (Objects.isNull(quitRequest) || Objects.isNull(loginUser)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = teamService.quitTeam(quitRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出失败");
        }
        return ResultUtils.success(true);
    }

    @PostMapping("/delete")
    @ApiOperation("解散队伍")
    public BaseResponse<Boolean> deleteTeam(@RequestBody TeamQuitRequest quitRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUserInfo(request);
        if (Objects.isNull(quitRequest) || Objects.isNull(loginUser)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = teamService.deleteTeam(quitRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解散失败");
        }
        return ResultUtils.success(true);
    }


}

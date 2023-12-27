package com.yu.usercenter.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yu.usercenter.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yu.usercenter.model.domain.User;
import com.yu.usercenter.model.domain.request.TeamAddRequest;
import com.yu.usercenter.model.domain.request.TeamJoinRequest;
import com.yu.usercenter.model.domain.request.TeamQuitRequest;
import com.yu.usercenter.model.domain.request.TeamUpdateRequest;
import com.yu.usercenter.model.dto.TeamQuery;
import com.yu.usercenter.model.vo.TeamUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface TeamService extends IService<Team> {

    Team createTeam(Team team, User loginUser);

    List<Team> selectTeamsByList(List<Long> ids);

    Page<TeamUserVO> listTeamsByPage(TeamQuery teamQuery, User loginUser);

    boolean updateTeam(TeamUpdateRequest updateRequest, User loginUser);

    boolean joinTeam(TeamJoinRequest joinRequest, User loginUser);

    boolean quitTeam(TeamQuitRequest quitRequest, User loginUser);

    boolean deleteTeam(TeamQuitRequest quitRequest, User loginUser);

    Page<TeamUserVO> listTeamsByPageJoin(TeamQuery teamQuery, Long userId);
}

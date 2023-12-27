package com.yu.usercenter.service.impl;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yu.usercenter.mapper.UserTeamMapper;
import com.yu.usercenter.model.domain.UserTeam;
import com.yu.usercenter.service.UserTeamService;
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam> implements UserTeamService{

}

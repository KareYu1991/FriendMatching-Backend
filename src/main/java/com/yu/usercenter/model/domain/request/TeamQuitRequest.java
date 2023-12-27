package com.yu.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户加入请求体
 */
@Data
public class TeamQuitRequest implements Serializable {

    private static final long serialVersionUID = -6043915331807008592L;

    /**
     * id
     */
    private Long teamId;

    /**
     * 退出用户的id
     */
    private Long userId;

    /**
     * 退出时间
     */
    private Date quitTime;
}
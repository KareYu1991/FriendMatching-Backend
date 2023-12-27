package com.yu.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
    * 用户队伍关系
    */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "user_team")
public class UserTeam implements Serializable {

    private static final long serialVersionUID = 6229199809870154144L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    @TableField(value = "userId")
    private Long userId;

    /**
     * 队伍id
     */
    @TableField(value = "teamId")
    private Long teamId;

    /**
     * 加入时间
     */
    @TableField(value = "joinTime")
    private Date joinTime;

    /**
     * 创建时间
     */
    @TableField(value = "createTime")
    private Date createTime;

    @TableField(value = "updateTime")
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Byte isDelete;

    public static final String COL_ID = "id";

    public static final String COL_USERID = "userId";

    public static final String COL_TEAMID = "teamId";

    public static final String COL_JOINTIME = "joinTime";

    public static final String COL_CREATETIME = "createTime";

    public static final String COL_UPDATETIME = "updateTime";

    public static final String COL_ISDELETE = "isDelete";
}
package com.yu.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
    * 队伍
    */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "team")
public class Team implements Serializable {
    private static final long serialVersionUID = -8809941394260363826L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 队伍名称
     */
    @TableField(value = "`name`")
    private String name;

    /**
     * 描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 最大人数
     */
    @TableField(value = "maxNum")
    private Integer maxNum;

    /**
     * 过期时间
     */
    @TableField(value = "expireTime")
    private Date expireTime;

    /**
     * 用户id（队长 id）
     */
    @TableField(value = "userId")
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    @TableField(value = "`status`")
    private Integer status;

    /**
     * 密码
     */
    @TableField(value = "`password`")
    private String password;

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

    public static final String COL_NAME = "name";

    public static final String COL_DESCRIPTION = "description";

    public static final String COL_MAXNUM = "maxNum";

    public static final String COL_EXPIRETIME = "expireTime";

    public static final String COL_USERID = "userId";

    public static final String COL_STATUS = "status";

    public static final String COL_PASSWORD = "password";

    public static final String COL_CREATETIME = "createTime";

    public static final String COL_UPDATETIME = "updateTime";

    public static final String COL_ISDELETE = "isDelete";
}
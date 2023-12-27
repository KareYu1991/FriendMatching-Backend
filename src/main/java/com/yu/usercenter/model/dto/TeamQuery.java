package com.yu.usercenter.model.dto;
import com.yu.usercenter.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TeamQuery extends PageRequest {
    private static final long serialVersionUID = 7344268478185843547L;
    /**
     * id
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 搜索内容
     */
    private String searchText;

    /**
     * 是否查看全部权限
     * 1是，其他不是
     */
    private Integer allSee = 0;
}
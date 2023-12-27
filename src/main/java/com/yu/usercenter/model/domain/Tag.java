package com.yu.usercenter.model.domain;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标签
 */
@Data
@TableName("tag")
public class Tag implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 标签名称
     */
    private String tagName;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 父标签 id
     */
    private Long parentId;

    /**
     * 0 - 不是, 1 - 父标签
     */
    private Byte isParent;

    /**
     * 创建时间
     */
    private Date createTime;

    private Date updateTime;

    /**
     * 是否删除
     */
    private Byte isDelete;


    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}
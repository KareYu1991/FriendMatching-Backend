package com.yu.usercenter.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = -4162304142710323660L;

    /**
     * 条数
     */
    protected int pageSize;

    /**
     * 当前是第几页
     */
    protected int pageNum;
}
package com.yu.usercenter.model.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 导入用户
 */
@Data
public class ImportUserDto implements Serializable {
    private Long id;

    private String username;

    private String point;

}

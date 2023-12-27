package com.yu.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TagSearch implements Serializable {

    // 标签名称搜索列表
    private List<String> tagNames;
}

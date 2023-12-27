package com.yu.usercenter.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import com.yu.usercenter.mapper.TagMapper;
import com.yu.usercenter.model.domain.Tag;
import com.yu.usercenter.service.TagService;

@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Resource
    private TagMapper tagMapper;

}

package com.yu.usercenter.service;

import cn.hutool.core.util.RandomUtil;
import com.yu.usercenter.model.domain.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class TagServiceTest {

    @Autowired
    private TagService tagService;


//    @Test
    public void initSaveBatch() {
        List<Tag> tags = new ArrayList<>();
//        for (int i = 0; i < 10; i++) {
//            Tag tag = new Tag();
//            tag.setId((long) i);
//            tag.setTagName(String.valueOf(RandomUtil.randomChar())+i);
//            tag.setIsparent((byte) 0);
//            tag.setParentid((long) 0);
//            tag.setUserid(1L);
//            tag.setIsdelete((byte) 0);
//            tags.add(tag);
//        }
        tagService.saveBatch(tags);
    }

}
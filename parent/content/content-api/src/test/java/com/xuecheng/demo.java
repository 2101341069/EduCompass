package com.xuecheng;

import com.xuecheng.content.mapper.CourseBaseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class demo {
    @Autowired
    private CourseBaseMapper courseBaseMapper;

    @Test
    public void test(){
        courseBaseMapper.deleteById(1);
    }
}

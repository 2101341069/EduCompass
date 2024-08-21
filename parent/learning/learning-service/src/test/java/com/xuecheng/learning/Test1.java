package com.xuecheng.learning;

import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/10/2 10:32
 */
public class Test1 {

    @Autowired
    XcChooseCourseMapper xcChooseCourseMapper;

    @Test
    public void test() {
     System.out.println("123".equals(null));
    }

}

package com.xuecheng.content;

import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.service.CoursePublishService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

@SpringBootTest
public class FeginUpLoadTest {

    @Autowired
    private MediaServiceClient mediaServiceClient;

    @Test
    public void test() throws IOException {

        File file=new File("D:\\xuecheng-plus-project\\xuecheng-plus-content\\xuecheng-plus-content-api\\src\\main\\resources\\templates\\course_template.ftl");
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        String updload = mediaServiceClient.updload(multipartFile, "course/120.html");
        System.out.println("updload===="+ updload);
    }
}

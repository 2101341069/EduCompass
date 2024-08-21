package com.xuecheng.content.feignclient;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
@Component
public class MediaServiceClientFallback implements MediaServiceClient{
    @Override
    public String updload(MultipartFile multipartFile, String objectName) throws IOException {
        return null;
    }
}

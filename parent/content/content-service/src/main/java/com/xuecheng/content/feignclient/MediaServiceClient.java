package com.xuecheng.content.feignclient;

import com.xuecheng.content.config.MultipartSupportConfig;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
@FeignClient(value = "media-api", configuration = MultipartSupportConfig.class,
        fallbackFactory = MediaServiceClientFallBackFactory.class
)
public interface MediaServiceClient {
    @ApiOperation("上传图片")
    @RequestMapping(value = "/media/upload/coursefile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String updload(@RequestPart("filedata") MultipartFile multipartFile, @RequestParam(value = "objectName", required = false) String objectName) throws IOException;
}

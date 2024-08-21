package com.xuecheng.content.feignclient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MediaServiceClientFallBackFactory implements FallbackFactory<MediaServiceClient> {
    @Override
    public MediaServiceClient create(Throwable throwable) {
        log.debug("远程调用上传文件接口发生异常，e：{}",throwable.getMessage());
        return new MediaServiceClientFallback();
    }
}

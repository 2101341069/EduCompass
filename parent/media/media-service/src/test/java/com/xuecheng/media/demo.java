package com.xuecheng.media;

import com.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.xuecheng.media.model.po.MediaProcessHistory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest
public class demo {
    @Autowired
    private MediaProcessHistoryMapper mediaProcessHistoryMapper;
    @Test
    public void test(){
        MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();
        mediaProcessHistory.setFailCount(12);
        mediaProcessHistory.setCreateDate(LocalDateTime.now());
        mediaProcessHistoryMapper.insert(mediaProcessHistory);
    }
}

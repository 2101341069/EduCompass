package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.model.po.MediaProcessHistory;
import com.xuecheng.media.service.MediaFileProcessService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MediaFileProcessServiceImpl implements MediaFileProcessService {
    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MediaProcessMapper mediaProcessMapper;

    @Autowired
    private MediaProcessHistoryMapper mediaProcessHistoryMapper;

    @Override
    public List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count) {
        List<MediaProcess> mediaProcesses = mediaProcessMapper.selectListByShardIndex(shardTotal, shardIndex, count);
        return mediaProcesses;
    }

    @Override
    public boolean startTask(long id) {
        int result = mediaProcessMapper.startTask(id);
        return result > 0;
    }

    @Override
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {
        MediaProcess mediaProcess = mediaProcessMapper.selectById(taskId);
        if (mediaProcess == null) {
            return;
        }
        if (status.equals("3")) {
//            mediaProcess.setStatus("3");
//            mediaProcess.setFailCount(mediaProcess.getFailCount()+1);
//            mediaProcess.setErrormsg(errorMsg);
            LambdaUpdateWrapper<MediaProcess> wrapper = new LambdaUpdateWrapper<>();
            wrapper.set(MediaProcess::getStatus, status);
            wrapper.set(MediaProcess::getFailCount, mediaProcess.getFailCount() + 1);
            wrapper.set(MediaProcess::getErrormsg, errorMsg);
            wrapper.eq(MediaProcess::getId, taskId);
            mediaProcessMapper.update(null, wrapper);
        }
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
        LambdaUpdateWrapper<MediaFiles> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(MediaFiles::getUrl, url);
        wrapper.eq(MediaFiles::getId, mediaFiles.getId());
        mediaFilesMapper.update(null, wrapper);


        mediaProcess.setStatus("2");
        mediaProcess.setUrl(url);
        mediaProcess.setFinishDate(LocalDateTime.now());

        LambdaUpdateWrapper<MediaProcess> updateWrapper = new LambdaUpdateWrapper<MediaProcess>();

        updateWrapper.set(MediaProcess::getStatus, "2");
        updateWrapper.set(MediaProcess::getFinishDate, LocalDate.now());
        updateWrapper.eq(MediaProcess::getId, mediaProcess.getId());

        mediaProcessMapper.update(null, updateWrapper);

        MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();


        BeanUtils.copyProperties(mediaProcess, mediaProcessHistory);

        mediaProcessHistoryMapper.insert(mediaProcessHistory);

        mediaProcessMapper.deleteById(mediaProcess.getId());
    }
}

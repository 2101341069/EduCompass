package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * XxlJob开发示例（Bean模式）
 * <p>
 * 开发步骤：
 * 1、任务开发：在Spring Bean实例中，开发Job方法；
 * 2、注解配置：为Job方法添加注解 "@XxlJob(value="自定义jobhandler名称", init = "JobHandler初始化方法", destroy = "JobHandler销毁方法")"，注解value值对应的是调度中心新建任务的JobHandler属性的值。
 * 3、执行日志：需要通过 "XxlJobHelper.log" 打印执行日志；
 * 4、任务结果：默认任务结果为 "成功" 状态，不需要主动设置；如有诉求，比如设置任务结果为失败，可以通过 "XxlJobHelper.handleFail/handleSuccess" 自主设置任务结果；
 *
 * @author xuxueli 2019-12-11 21:52:51
 */
@Component
public class VideoTask {


    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegpath;
    private static Logger logger = LoggerFactory.getLogger(VideoTask.class);


    @Autowired
    private MediaFileService mediaFileService;

    @Autowired
    private MediaFileProcessService processService;

    /**
     * 视频处理
     *
     * @throws Exception
     */
    @XxlJob("videoJobHandler")
    public void demoJobHandler() throws Exception {
        XxlJobHelper.log("XXL-JOB, Hello World.");
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        //cpu核心数
        int processors = Runtime.getRuntime().availableProcessors();
        List<MediaProcess> mediaProcessList = processService.getMediaProcessList(shardIndex, shardTotal, processors);
        int size = mediaProcessList.size();
        logger.debug("取到的视频处理任务数：" + size);
        if (size <= 0) {
            return;
        }
        //创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(size);
        CountDownLatch countDownLatch = new CountDownLatch(size);
        mediaProcessList.forEach(mediaProcess -> {
            executorService.execute(() -> {
                try {
                    //开启任务
                    Long id = mediaProcess.getId();
                    boolean taskFlag = processService.startTask(id);
                    if (!taskFlag) {
                        logger.debug("抢占任务失败，任务id：{}", id);
                        return;
                    }
                    //执行视频转码
                    //下载文件到本地
                    String bucket = mediaProcess.getBucket();
                    String objectName = mediaProcess.getFilePath();
                    File file = mediaFileService.downloadFileFromMinIO(bucket, objectName);
                    if (file == null) {
                        logger.debug("下载视频出错，任务id：{}，bucket：{}，objectName：{}", id, bucket, objectName);
                        processService.saveProcessFinishStatus(id, "3", mediaProcess.getFileId(), null, "下载视频出错");
                        return;
                    }
                    String mp4Name = mediaProcess.getFileId();
                    //源avi视频的路径
                    String video_path = file.getAbsolutePath();
                    //转换后mp4文件的名称
                    String mp4_name = mp4Name + ".mp4";
                    //转换后mp4文件的路径
                    File tempFile = null;
                    try {
                        tempFile = File.createTempFile("minio", ".mp4");
                    } catch (IOException e) {
                        logger.debug("临时文件创建失败，原因：{}", e.getMessage());
                        processService.saveProcessFinishStatus(id, "3", mediaProcess.getFileId(), null, "临时文件创建失败");
                        return;
                    }
                    String mp4_path = tempFile.getAbsolutePath();
                    //创建工具类对象
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegpath, video_path, mp4_name, mp4_path);
                    //开始视频转换，成功将返回success
                    String s = videoUtil.generateMp4();
                    if (!s.equals("success")) {
                        logger.debug("视频转码失败，bucket：{}，objectname：{}", bucket, objectName);
                        processService.saveProcessFinishStatus(id, "3", mediaProcess.getFileId(), null, s);
                        return;
                    }
                    //mp4在minio的存储路径
                     objectName = getFilePath(mediaProcess.getFileId(), ".mp4");
                    //访问url
                    String url = "/" + bucket + "/" + objectName;
                    //上传到minio
                    boolean uploadMinio = mediaFileService.addMediFileToMinIo(bucket, "video/mp4", objectName, tempFile.getAbsolutePath());
                    if (!uploadMinio) {
                        logger.debug("上传minio失败，taskId：{}", id);
                        processService.saveProcessFinishStatus(id, "3", mediaProcess.getFileId(), null, "上传minio失败");
                        return;
                    }

                    //保存任务出来结果
                    processService.saveProcessFinishStatus(id, "2", mediaProcess.getFileId(), url, "");
                } finally {
                    countDownLatch.countDown();
                }

            });
        });
        countDownLatch.await(30, TimeUnit.MINUTES);
        executorService.shutdown();
    }

    private String getFilePath(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }
}

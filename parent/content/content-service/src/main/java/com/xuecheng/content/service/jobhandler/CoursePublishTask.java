package com.xuecheng.content.service.jobhandler;

import com.xuecheng.base.execption.XueChengPlusException;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.model.po.CourseIndex;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {


    @Autowired
    private CoursePublishService coursePublishService;


    @Autowired
    private SearchServiceClient searchServiceClient;

    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception{
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        this.process(shardIndex,shardTotal,"course_publish",30,60);
    }

    @Override
    public boolean execute(MqMessage mqMessage) {
        //得到课程id
        Long courseId=Long.valueOf(mqMessage.getBusinessKey1());
        generateCourseHtml(mqMessage,courseId);
        saveCourseIndex(mqMessage,courseId);
        return true;
    }

    //生成课程静态化页面上传到文件系统

    private void generateCourseHtml(MqMessage mqMessage,long courseId){
        //做任务幂等性处理
        //取出该阶段的执行状态
        Long taskId=mqMessage.getId();

        MqMessageService mqMessageService = this.getMqMessageService();
        int stageOne = mqMessageService.getStageOne(taskId);
        if (stageOne>0){
            log.debug("静态化课程任务完成，无需处理");
            return;
        }
        //开始课程静态页面
        File file = coursePublishService.generateCourseHtml(courseId);
        if (file==null){
            XueChengPlusException.cast("生成的静态页面为null");
        }
        coursePublishService.uploadCourseHtml(courseId,file);

        //处理任务完成
        mqMessageService.completedStageOne(taskId);
    }

    //保存课程索引信息进es中
    //第二阶段任务
    private void saveCourseIndex(MqMessage mqMessage,long courseId){
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        int stageTwo = mqMessageService.getStageTwo(taskId);
        if (stageTwo>0){
            log.debug("保存课程索引信息进es中完成");
            return;
        }
        CoursePublish coursePublish = coursePublishService.getById(courseId);
        if (coursePublish==null){
            XueChengPlusException.cast("没有该课程信息");
        }
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish,courseIndex);
        Boolean add = searchServiceClient.add(courseIndex);
        if (!add){
            XueChengPlusException.cast("远程调用课程索引失败");
        }
        //完成保存课程索引信息进es中
        mqMessageService.completedStageTwo(taskId);
    }

}

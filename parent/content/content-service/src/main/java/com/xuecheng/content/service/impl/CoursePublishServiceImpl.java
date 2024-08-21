package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.execption.CommonError;
import com.xuecheng.base.execption.XueChengPlusException;
import com.xuecheng.base.utils.StringUtil;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.CoursePublishPre;
import com.xuecheng.content.service.*;
import com.xuecheng.messagesdk.mapper.MqMessageMapper;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 课程发布 服务实现类
 * </p>
 *
 * @author itcast
 */
@Slf4j
@Service
public class CoursePublishServiceImpl extends ServiceImpl<CoursePublishMapper, CoursePublish> implements CoursePublishService {

    @Autowired
    private CourseBaseService courseBaseService;

    @Autowired
    private CourseTeacherService courseTeacherService;
    @Autowired
    private TeachplanService teachplanService;

    @Autowired
    private CoursePublishPreMapper coursePublishPreMapper;
    @Autowired
    private CourseBaseMapper courseBaseMapper;

    @Autowired
    private CoursePublishMapper coursePublishMapper;

    @Resource
    private MqMessageService mqMessageService;

    @Autowired
    private MediaServiceClient mediaServiceClient;


    @Autowired
    private RedisTemplate redisTemplate;


    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        CourseBaseInfoDto courseBaseInfoDto = courseBaseService.getCourseBaseById(courseId);

        List<TeachplanDto> teachplanDtoList = teachplanService.selectTreeNodes(courseId);
        coursePreviewDto.setCourseBase(courseBaseInfoDto);
        coursePreviewDto.setTeachplans(teachplanDtoList);
        return coursePreviewDto;
    }

    @Transactional
    @Override
    public void commitAudit(Long courseId, Long companyId) {
        CourseBaseInfoDto courseBaseInfoDto =
                courseBaseService.getCourseBaseById(courseId);
        if (courseBaseInfoDto == null) {
            XueChengPlusException.cast("课程找不到");
        }

        //审核状态
        if ("202003".equals(courseBaseInfoDto.getAuditStatus())) {
            XueChengPlusException.cast("课程已经提交请等待审核");
        }
        //课程图片计划信息没有填写不允许提交
        String pic = courseBaseInfoDto.getPic();
        if (StringUtil.isEmpty(pic)) {
            XueChengPlusException.cast("请上传图片");
        }

        List<TeachplanDto> teachplanDtoList = teachplanService.selectTreeNodes(courseId);
        if (teachplanDtoList == null || teachplanDtoList.size() == 0) {
            XueChengPlusException.cast("请编写课程计划");
        }
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        coursePublishPre.setCompanyId(companyId);
        BeanUtils.copyProperties(courseBaseInfoDto, coursePublishPre);
        List marketList = Arrays.asList(courseBaseInfoDto.getCharge(), courseBaseInfoDto.getPrice()
                , courseBaseInfoDto.getOriginalPrice(),
                courseBaseInfoDto.getQq(), courseBaseInfoDto.getWechat(),
                courseBaseInfoDto.getPhone(), courseBaseInfoDto.getValidDays(),
                courseBaseInfoDto.getMtName(), courseBaseInfoDto.getStName());
        String marketJSONStr = JSON.toJSONString(marketList);
        coursePublishPre.setMarket(marketJSONStr);
        //计划信息
        String teachplanJSONStr = JSON.toJSONString(teachplanDtoList);
        coursePublishPre.setTeachplan(teachplanJSONStr);
        //老师信息
        coursePublishPre.setStatus("202003");
        coursePublishPre.setCreateDate(LocalDateTime.now());
        CoursePublishPre coursePublishPreObj = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPreObj == null) {
            coursePublishPreMapper.insert(coursePublishPre);
        } else {
            coursePublishPreMapper.updateById(coursePublishPre);
        }
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBase);
    }


    @Transactional
    @Override
    public void publish(Long companyId, Long courseId) {
        //查询预发布表
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre == null) {
            XueChengPlusException.cast("课程没有发布");
        }
        //课程审核
        String status = coursePublishPre.getStatus();
        if (!"202004".equals(status)) {
            XueChengPlusException.cast("课程没有审核不允许发布");
        }
        //像课程发布表里写数据
        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre, coursePublish);

        CoursePublish coursePublishObj = coursePublishMapper.selectById(courseId);
        if (coursePublishObj == null) {
            coursePublishMapper.insert(coursePublish);
        } else {
            coursePublishMapper.updateById(coursePublish);
        }


        //保存消息表
        saveCoursePublishMessage(courseId);
        //跟该课程状态为已发布
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setStatus("203002");
        courseBaseMapper.updateById(courseBase);
        //课程发布之后删除预发布表记录
        coursePublishPreMapper.deleteById(courseId);
    }

    @Override
    public File generateCourseHtml(Long courseId) {
        CoursePreviewDto coursePreviewInfo = this.getCoursePreviewInfo(courseId);
        Configuration configuration = new Configuration(Configuration.getVersion());
        String path = this.getClass().getResource("/").getPath();
        File htmFile = null;
        FileOutputStream stream = null;
        try {
            configuration.setDirectoryForTemplateLoading(new File(path + "/templates/"));
            configuration.setDefaultEncoding("utf-8");
            Template template = configuration.getTemplate("course_template.ftl");
            HashMap<String, Object> model = new HashMap<>();
            model.put("model", coursePreviewInfo);
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
            htmFile = File.createTempFile("couresepublish", ".html");
            stream = new FileOutputStream(htmFile);

            InputStream inputStream = IOUtils.toInputStream(html, "utf-8");
            IOUtils.copy(inputStream, stream);


        } catch (Exception e) {
            log.error("课程静态化出现问题，课程id：{}", courseId, e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    log.debug("输出流关闭异常", e);
                }
            }
        }
        return htmFile;
    }

    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        try {
            MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
            String updload = mediaServiceClient.updload(multipartFile, "course/" + courseId + ".html");
            if (updload == null) {
                log.debug("远程调用走降级逻辑上传结果为null，课程id为：{}", courseId);
                XueChengPlusException.cast("远程调用走降级逻辑上传结果为null");
            }
        } catch (Exception e) {
            e.printStackTrace();
            XueChengPlusException.cast("上传静态文件过程中存在异常,异常为:{}" + e.getMessage());
        }
    }

    @Override
    public CoursePublish getCoursePublish(Long courseId) {
        Object jsonObj = redisTemplate.opsForValue().get("course:" + courseId);
        if (jsonObj != null) {
            String jsonString = jsonObj.toString();
            if ("null".equals(jsonString)){
                return null;
            }
            CoursePublish coursePublish = JSON.parseObject(jsonString, CoursePublish.class);
            return coursePublish;
        } else {

            synchronized (this) {
                jsonObj = redisTemplate.opsForValue().get("course:" + courseId);
                if (jsonObj != null) {
                    String jsonString = jsonObj.toString();
                    if ("null".equals(jsonString)){
                        return null;
                    }
                    CoursePublish coursePublish = JSON.parseObject(jsonString, CoursePublish.class);
                    return coursePublish;
                }
                CoursePublish coursePublish = this.baseMapper.selectById(courseId);
//                if (coursePublish != null)
                    redisTemplate.opsForValue().set("course:" + courseId, JSON.toJSONString(coursePublish),30, TimeUnit.SECONDS);
                return coursePublish;
            }

        }

    }

    private void saveCoursePublish(Long courseId) {
        //整合课程发布信息
        //查询课程预发布表
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre == null) {
            XueChengPlusException.cast("课程预发布数据为空");
        }
    }

    /**
     * @param courseId 课程id
     * @return void
     * @description 保存消息表记录，稍后实现
     * @author Mr.M
     * @date 2022/9/20 16:32
     */
    private void saveCoursePublishMessage(Long courseId) {
        MqMessage mqMessage =
                mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if (mqMessage == null) {
            XueChengPlusException.cast(CommonError.UNKOWN_ERROR);
        }
    }
}

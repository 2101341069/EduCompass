package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.execption.XueChengPlusException;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 选课相关接口实现
 */
@Slf4j
@Service
public class MyCourseTablesServiceImpl implements MyCourseTablesService {

    @Resource
    private XcChooseCourseMapper chooseCourseMapper;

    @Resource
    private XcCourseTablesMapper courseTablesMapper;

    @Autowired
    private ContentServiceClient contentServiceClient;

    @Autowired
    private MyCourseTablesServiceImpl courseTablesService;
    @Transactional
    @Override
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        //选课调用内容管理查询课程的收费规则
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish == null) {
            XueChengPlusException.cast("课程不存在");
        }
        String charge = coursepublish.getCharge();
        XcChooseCourse xcChooseCourse = null;
        if ("201000".equals(charge)) {//免费课程直接加入我的课程中
            xcChooseCourse = addFreeCourse(userId, coursepublish);
            XcCourseTables xcCourseTables = addCourseTables(xcChooseCourse);
        } else {//收费课程
            xcChooseCourse = addChargeCourse(userId, coursepublish);
        }
        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(xcChooseCourse,xcChooseCourseDto);
        XcCourseTablesDto learningStatus = getLearningStatus(userId, courseId);
        xcChooseCourseDto.setLearnStatus(learningStatus.getLearnStatus());
        return xcChooseCourseDto;
    }

    public XcCourseTablesDto getLearningStatus(String userId, Long courseId){
        XcCourseTables courseTables = getCourseTables(userId, courseId);
        //学习资格，[{"code":"702001","desc":"正常学习"},
        // {"code":"702002","desc":"没有选课或选课后没有支付"},
        // {"code":"702003","desc":"已过期需要申请续期或重新支付"}]
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();

        if (courseTables==null){
            xcCourseTablesDto.setLearnStatus("702002");
            return xcCourseTablesDto;
        }
        BeanUtils.copyProperties(courseTables, xcCourseTablesDto);
        if (courseTables.getValidtimeEnd().isBefore(LocalDateTime.now())){
            xcCourseTablesDto.setLearnStatus("702003");
            return xcCourseTablesDto;
        }else {
            xcCourseTablesDto.setLearnStatus("702001");
            return xcCourseTablesDto;
        }
    }

    @Override
    public boolean saveChooseSuccess(String choseCourseId) {
        XcChooseCourse xcChooseCourse = chooseCourseMapper.selectById(choseCourseId);
        if (xcChooseCourse==null){
            log.debug("接受购买课程的消息，根据选课id从数据库里找不到选课记录，选课id：{}",choseCourseId);
            return false;
        }
        String status = xcChooseCourse.getStatus();
        if ("701002".equals(status)){
            xcChooseCourse.setStatus("701001");
            int update = chooseCourseMapper.updateById(xcChooseCourse);
            if (update<=0){
                log.debug("添加选课记录失败，选课id：{}",choseCourseId);
                XueChengPlusException.cast("添加选课记录失败");
            }
           courseTablesService.addCourseTables(xcChooseCourse);
            return true;
        }
        return false;
    }

    @Override
    public PageResult<XcCourseTables> mycoursetable(MyCourseTableParams params) {
        int page = params.getPage();
        int size = params.getSize();
        Page<XcCourseTables> CourseTablesPage = new Page<>(page,size);
        LambdaUpdateWrapper<XcCourseTables> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(XcCourseTables::getUserId,params.getUserId());
        Page<XcCourseTables> courseTablesPage = courseTablesMapper.selectPage(CourseTablesPage, wrapper);
        PageResult<XcCourseTables> pageResult = new PageResult<>();
        long pages = CourseTablesPage.getPages();
        pageResult.setPage(pages);
        long total = courseTablesPage.getTotal();
        pageResult.setCounts(total);
        pageResult.setItems(courseTablesPage.getRecords());
        return pageResult;
    }

    //添加免费课程，将免费课程加入选课记录、我的课程表
    public XcChooseCourse addFreeCourse(String userId, CoursePublish coursePublish) {
        Long courseId = coursePublish.getId();
        //查询是否已经加入课程
        LambdaUpdateWrapper<XcChooseCourse> wrapper = new LambdaUpdateWrapper<XcChooseCourse>()
                .eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, courseId)
                .eq(XcChooseCourse::getOrderType, "700001")
                .eq(XcChooseCourse::getStatus, "701001");
        List<XcChooseCourse> chooseCourses = chooseCourseMapper.selectList(wrapper);
        if (chooseCourses.size() > 0) {
            return chooseCourses.get(0);
        }

        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(courseId);
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCourseName(coursePublish.getName());
        xcChooseCourse.setCompanyId(coursePublish.getCompanyId());
        xcChooseCourse.setValidDays(365);
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setCoursePrice(coursePublish.getPrice());
        xcChooseCourse.setStatus("701001");
        xcChooseCourse.setOrderType("700001");
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));
        int insert = chooseCourseMapper.insert(xcChooseCourse);
        if (insert <= 0) {
            XueChengPlusException.cast("添加选课失败");
        }
        return xcChooseCourse;
    }

    //添加收费课程
    public XcChooseCourse addChargeCourse(String userId, CoursePublish coursePublish) {
        Long courseId = coursePublish.getId();
        //判断是否存在收费课程记录且为选课记录为待支付，直接返回
        LambdaUpdateWrapper<XcChooseCourse> wrapper = new LambdaUpdateWrapper<XcChooseCourse>()
                .eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, courseId)
                .eq(XcChooseCourse::getOrderType, "700002")
                .eq(XcChooseCourse::getStatus, "701002");
        List<XcChooseCourse> chooseCourses = chooseCourseMapper.selectList(wrapper);
        if (chooseCourses.size() > 0) {
            return chooseCourses.get(0);
        }

        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(courseId);
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCourseName(coursePublish.getName());
        xcChooseCourse.setCompanyId(coursePublish.getCompanyId());
        xcChooseCourse.setValidDays(365);
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setCoursePrice(coursePublish.getPrice());
        xcChooseCourse.setStatus("701002");
        xcChooseCourse.setOrderType("700002");
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));
        int insert = chooseCourseMapper.insert(xcChooseCourse);
        if (insert <= 0) {
            XueChengPlusException.cast("添加选课失败");
        }
        return xcChooseCourse;
    }

    //添加到我的课程表
    @Transactional
    public XcCourseTables addCourseTables(XcChooseCourse xcChooseCourse) {

        String status = xcChooseCourse.getStatus();
        if (!"701001".equals(status)) {
            XueChengPlusException.cast("选课没有成功，无法加入我的课程");
        }
        XcCourseTables xcCourseTables = getCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if (xcCourseTables != null) {
            return xcCourseTables;
        }
        xcCourseTables=new XcCourseTables();
        BeanUtils.copyProperties(xcChooseCourse,xcCourseTables);
        xcCourseTables.setChooseCourseId(xcChooseCourse.getId());
        xcCourseTables.setId(null);
        xcCourseTables.setCourseType(xcChooseCourse.getOrderType());
        xcCourseTables.setUpdateDate(LocalDateTime.now());
        int insert = courseTablesMapper.insert(xcCourseTables);
        if (insert<=0){
            XueChengPlusException.cast("添加课程失败");
        }

        return xcCourseTables;
    }
    private XcCourseTables getCourseTables(String userId,Long courseId){
        LambdaUpdateWrapper<XcCourseTables> wrapper = new LambdaUpdateWrapper<XcCourseTables>()
                .eq(XcCourseTables::getCourseId, courseId)
                .eq(XcCourseTables::getUserId,userId);
        XcCourseTables xcCourseTables = courseTablesMapper.selectOne(wrapper);
        return xcCourseTables;
    }
}

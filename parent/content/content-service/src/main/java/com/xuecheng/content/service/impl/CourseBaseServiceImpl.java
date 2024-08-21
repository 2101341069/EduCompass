package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.execption.RestErrorResponse;
import com.xuecheng.base.execption.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.*;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

/**
 * <p>
 * 课程基本信息 服务实现类
 * </p>
 *
 * @author itcast
 */
@Slf4j
@Validated
@Service
public class CourseBaseServiceImpl extends ServiceImpl<CourseBaseMapper, CourseBase> implements CourseBaseService {


    @Autowired
    private CourseCategoryMapper courseCategoryMapper;
    @Autowired
    private CourseMarketService courseMarketService;

    @Autowired
    private TeachplanService teachplanService;

    @Autowired
    private CourseTeacherService teacherService;


    @Autowired
    private TeachplanMediaService teachplanMediaService;

    @Override
    public PageResult<CourseBase> queryByCondtionPage(Long companyId, PageParams pageParams,
                                                      QueryCourseParamsDto queryCourseParamsDto) {
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        LambdaQueryWrapper<CourseBase> wrapper = new LambdaQueryWrapper<>();

        wrapper.like(!StringUtils.isEmpty(queryCourseParamsDto.getCourseName()),
                CourseBase::getName, queryCourseParamsDto.getCourseName());
        wrapper.eq(!StringUtils.isEmpty(queryCourseParamsDto.getAuditStatus()),
                CourseBase::getAuditStatus, queryCourseParamsDto.getAuditStatus());
        wrapper.eq(!StringUtils.isEmpty(queryCourseParamsDto.getPublishStatus()),
                CourseBase::getAuditStatus, queryCourseParamsDto.getPublishStatus());
        wrapper.eq(CourseBase::getCompanyId,companyId);
        Page<CourseBase> courseBasePage = baseMapper.selectPage(page, wrapper);

        return new PageResult<CourseBase>(courseBasePage.getRecords(), page.getTotal(), page.getPages(), page.getSize());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto) {


        CourseBase courseBase = new CourseBase();

        BeanUtils.copyProperties(addCourseDto, courseBase);
        courseBase.setCompanyId(companyId);
        courseBase.setCreateDate(LocalDateTime.now());
        courseBase.setAuditStatus("202002");
        courseBase.setSt("203001");
        int insert = baseMapper.insert(courseBase);
        if (insert <= 0) {
            throw new RuntimeException("添加课程失败");
        }
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(addCourseDto, courseMarket);
        Long courseId = courseBase.getId();
        courseMarket.setId(courseId);
        courseMarketService.saveOrUpdate(courseMarket);
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseBase.getId());
        return courseBaseInfo;
    }


    @Override
    public CourseBaseInfoDto getCourseBaseById(Long id) {
        CourseBase courseBase = this.baseMapper.selectById(id);
        CourseMarket courseMarket = courseMarketService.getById(id);
        CourseBaseInfoDto baseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase, baseInfoDto);
        if (courseMarket != null) {
            BeanUtils.copyProperties(courseMarket, baseInfoDto);
        }
        CourseCategory courseCategory = courseCategoryMapper.selectById(courseBase.getMt());

        baseInfoDto.setMtName(courseCategory.getName());

        CourseCategory courseCategory1 = courseCategoryMapper.selectById(courseBase.getSt());
        baseInfoDto.setStName(courseCategory1.getName());

        return baseInfoDto;
    }

    @Transactional(rollbackFor = {RuntimeException.class, XueChengPlusException.class})
    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto) {
        Long courseId = editCourseDto.getId();
        CourseBase courseBase = baseMapper.selectById(courseId);
        if (courseBase == null) {
            throw new XueChengPlusException("课程不存在");
        }
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();

        BeanUtils.copyProperties(editCourseDto, courseBase);
        courseBase.setChangeDate(LocalDateTime.now());
        BeanUtils.copyProperties(editCourseDto, courseBaseInfoDto);
        int i = this.baseMapper.updateById(courseBase);
        if (i <= 0) {
            throw new XueChengPlusException("修改课程失败");
        }

        CourseMarket courseMarket = courseMarketService.getById(courseId);
        if (courseMarket == null) {
            courseMarket = new CourseMarket();
        }
        BeanUtils.copyProperties(editCourseDto, courseMarket);
        boolean b = courseMarketService.saveOrUpdate(courseMarket);
        if (!b) {
            throw new XueChengPlusException("修改课程失败");
        }


        return courseBaseInfoDto;
    }

    @Override
    public RestErrorResponse deleteByCourseId(Long courseId) {
        CourseBase courseBase = baseMapper.selectById(courseId);
        if (!courseBase.getStatus().equals("203001")) {
            return new RestErrorResponse("课程已经发布，无法删除");
        }
        baseMapper.deleteById(courseId);
        teacherService.remove(new LambdaQueryWrapper<CourseTeacher>().eq(CourseTeacher::getCourseId, courseId));
        teachplanMediaService.remove(new LambdaQueryWrapper<TeachplanMedia>().eq(TeachplanMedia::getCourseId, courseId));
        teachplanService.remove(new LambdaQueryWrapper<Teachplan>().eq(Teachplan::getCourseId, courseId));
        RestErrorResponse restErrorResponse = new RestErrorResponse("");
        restErrorResponse.setErrCode("200");
        return restErrorResponse;
    }

    public CourseBaseInfoDto getCourseBaseInfo(long courseId) {

        CourseBase courseBase = baseMapper.selectById(courseId);
        if (courseBase == null) {
            return null;
        }
        CourseBaseInfoDto baseInfoDto = new CourseBaseInfoDto();
        CourseMarket courseMarket = courseMarketService.getById(courseId);
        BeanUtils.copyProperties(courseBase, baseInfoDto);
        int i=1/0;
        if (courseMarket != null)
            BeanUtils.copyProperties(courseMarket, baseInfoDto);
        return baseInfoDto;
    }
}

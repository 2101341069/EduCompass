package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.base.execption.RestErrorResponse;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.*;
import com.xuecheng.content.model.po.CourseBase;

/**
 * <p>
 * 课程基本信息 服务类
 * </p>
 *
 * @author itcast
 * @since 2024-03-05
 */
public interface CourseBaseService extends IService<CourseBase> {

    PageResult<CourseBase> queryByCondtionPage(Long companyId,PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto);

   CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto);

    CourseBaseInfoDto getCourseBaseById(Long id);

    CourseBaseInfoDto updateCourseBase(Long l, EditCourseDto editCourseDto);

    RestErrorResponse deleteByCourseId(Long courseId);
}

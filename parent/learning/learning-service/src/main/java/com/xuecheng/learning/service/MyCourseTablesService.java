package com.xuecheng.learning.service;

import com.xuecheng.base.model.PageResult;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcCourseTables;

public interface MyCourseTablesService {
    /**
     * 添加选课
     * @param userId 用户id
     * @param courseId 课程id
     * @return
     */
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId);

    /**
     * 获取用户对某个课程的学习状态
     * @param userId 用户id
     * @param courseId 课程id
     * @return
     */
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId);


    /**
     * 保存选课成功状态
     * @param choseCourseId 选课id
     * @return
     */
    public boolean saveChooseSuccess(String choseCourseId);

    PageResult<XcCourseTables> mycoursetable(MyCourseTableParams params);
}

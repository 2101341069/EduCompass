package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 课程分类 服务实现类
 * </p>
 *
 * @author itcast
 */
@Slf4j
@Service
public class CourseCategoryServiceImpl extends ServiceImpl<CourseCategoryMapper, CourseCategory> implements CourseCategoryService {

    @Override
    public List<CourseCategoryTreeDto> findAllCourseCategory() {
        List<CourseCategory> courseCategories = baseMapper.selectList(null);
        List<CourseCategoryTreeDto> buildTreeCourseCategory = buildTreeCourseCategory(courseCategories);
        CourseCategoryTreeDto courseCategoryTreeDto = buildTreeCourseCategory.get(0);

        return courseCategoryTreeDto.getChildrenTreeNodes();
    }

    private List<CourseCategoryTreeDto> buildTreeCourseCategory(List<CourseCategory> list) {
        List<CourseCategoryTreeDto> result = new ArrayList<>();
        for (CourseCategory courseCategory : list) {
            if (courseCategory.getParentid().equals("0")) {
                CourseCategoryTreeDto courseCategoryTreeDto = buildTreeCourseCategoryTrace(list, courseCategory);
                result.add(courseCategoryTreeDto);
            }
        }
        return result;
    }

    private CourseCategoryTreeDto buildTreeCourseCategoryTrace(List<CourseCategory> list, CourseCategory category) {
        CourseCategoryTreeDto courseCategoryTreeDto = new CourseCategoryTreeDto();
        BeanUtils.copyProperties(category,courseCategoryTreeDto);
        courseCategoryTreeDto.setChildrenTreeNodes(new ArrayList<>());
        for (CourseCategory courseCategory : list) {
            if (courseCategory.getParentid().equals(category.getId())) {
                courseCategoryTreeDto.getChildrenTreeNodes().add(buildTreeCourseCategoryTrace(list,courseCategory));
            }
        }
        return courseCategoryTreeDto;
    }
}

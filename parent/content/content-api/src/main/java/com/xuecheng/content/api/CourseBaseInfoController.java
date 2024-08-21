package com.xuecheng.content.api;

import com.xuecheng.base.execption.RestErrorResponse;
import com.xuecheng.base.execption.ValidationGroups;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.content.model.dto.*;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseService;
import com.xuecheng.content.service.CourseCategoryService;
import com.xuecheng.content.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Api(value = "课程信息管理接口", tags = "课程信息管理接口")
@RestController
public class CourseBaseInfoController {

    @Autowired
    private CourseBaseService courseBaseService;


    @Autowired
    private CourseCategoryService courseCategoryService;

    @ApiOperation("课程查询接口")
    @PreAuthorize("hasAuthority('xc_teachmanager_course_list')")
    @PostMapping("/course/list")
    public PageResult<CourseBase> list(PageParams pageParams, @RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto) {
        SecurityUtil.XcUser user = SecurityUtil.getUser();

        Long companyId=null;
        if (user!=null&&!StringUtils.isEmpty(user.getCompanyId())){
            companyId=Long.valueOf(user.getCompanyId());
        }else {
            throw new RuntimeException("不是机构成员，无法查询");
        }

        PageResult<CourseBase> pageResult =
                courseBaseService.queryByCondtionPage(companyId,pageParams, queryCourseParamsDto);
        return pageResult;
    }
    @ApiOperation("课程分类查询")
    @GetMapping("/course-category/tree-nodes")
    public List<CourseCategoryTreeDto> courseBaseCategory() {
        List<CourseCategoryTreeDto> categoryTreeDtoPageResult=
                courseCategoryService.findAllCourseCategory();
        return categoryTreeDtoPageResult;
    }

    @ApiOperation("新增课程")
    @PostMapping("/course")
    public CourseBaseInfoDto createCourseBase(
            @RequestBody @Validated(value = ValidationGroups.Inster.class)
                                                  AddCourseDto addCourseDto){
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        Long companyId=null;
        if (user!=null&&!StringUtils.isEmpty(user.getCompanyId())){
            companyId=Long.valueOf(user.getCompanyId());
        }else {
            throw new RuntimeException("不是机构成员，无法查询");
        }
       CourseBaseInfoDto baseInfoDto= courseBaseService.createCourseBase(123l,addCourseDto);
        return baseInfoDto;
    }
    @ApiOperation("获得课程信息")
    @GetMapping("/course/{id}")
    public CourseBaseInfoDto gerCourseBaseById(@PathVariable("id")Long id){
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        System.out.println(user.getUsername());
        CourseBaseInfoDto courseBaseInfoDto=  courseBaseService.getCourseBaseById(id);
        return courseBaseInfoDto;
    }
    @ApiOperation("修改课程信息")
    @PutMapping("/course")
    public CourseBaseInfoDto updateCourseBaseInfo(@RequestBody @Validated(ValidationGroups.Update.class) EditCourseDto editCourseDto){

        SecurityUtil.XcUser user = SecurityUtil.getUser();
        Long companyId=null;
        if (!StringUtils.isEmpty(user.getCompanyId())){
            companyId=Long.valueOf(user.getCompanyId());
        }else {
            throw new RuntimeException("不是机构成员，无法查询");
        }
        CourseBaseInfoDto courseBaseInfoDto=  courseBaseService.updateCourseBase(companyId,editCourseDto);
        return courseBaseInfoDto;
    }
    @ApiOperation("删除课程")
    @DeleteMapping("/course/{id}")
    public RestErrorResponse remove(@PathVariable("id")Long courseId){
        RestErrorResponse res=   courseBaseService.deleteByCourseId(courseId);
        return res;
    }

}

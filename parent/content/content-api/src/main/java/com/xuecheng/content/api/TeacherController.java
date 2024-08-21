package com.xuecheng.content.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.execption.RestErrorResponse;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.service.CourseTeacherService;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@Api(value = "老师管理接口")
public class TeacherController {

    @Autowired
    private CourseTeacherService teacherService;


    @ApiOperation("老师查询")
    @GetMapping("/courseTeacher/list/{id}")
    public List<CourseTeacher> getTeacherInfo(@PathVariable("id") Long courseId) {
        LambdaQueryWrapper<CourseTeacher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseTeacher::getCourseId, courseId);
        List<CourseTeacher> list = teacherService.list(wrapper);
        return list;
    }

    @ApiOperation("添加老师信息")
    @PostMapping("/courseTeacher")
    public CourseTeacher save(@RequestBody CourseTeacher courseTeacher) {
        boolean save = teacherService.saveOrUpdate(courseTeacher);
        if (save){

            return courseTeacher;
        }
        return null;
    }

    @ApiOperation("删除老师信息")
    @DeleteMapping("/courseTeacher/course/{courseId}/{teacherId}")
    public RestErrorResponse remove(@PathVariable("courseId")Long courseId
            ,@PathVariable("teacherId")Long teacherId){
        LambdaQueryWrapper<CourseTeacher> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(CourseTeacher::getCourseId,courseId).eq(CourseTeacher::getId,teacherId);
        boolean remove = teacherService.remove(wrapper);
        if (remove){
            RestErrorResponse restErrorResponse = new RestErrorResponse("");
            restErrorResponse.setErrCode("200");
            return restErrorResponse;
        }else {
            RestErrorResponse restErrorResponse = new RestErrorResponse("删除失败");
            restErrorResponse.setErrCode("400");
            return restErrorResponse;
        }
    }
}

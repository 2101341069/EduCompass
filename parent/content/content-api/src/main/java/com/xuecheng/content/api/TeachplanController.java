package com.xuecheng.content.api;

import com.xuecheng.base.execption.RestErrorResponse;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(value = "课程计划编辑接口", tags = "课程计划编辑接口")
@RestController
public class TeachplanController {

    @Autowired
    private TeachplanService teachplanService;


    @ApiOperation(value = "课程计划和媒资信息绑定")
    @PostMapping("/teachplan/association/media")
    public void associationMedia(@RequestBody BindTeachplanMediaDto bindTeachplanMediaDto) {
            teachplanService.associationMedia(bindTeachplanMediaDto);
    }

    @ApiOperation("查询课程计划树形结构")
    @ApiImplicitParam(value = "courseId", name = "课程Id", required = true, dataType = "Long", paramType = "path")
    @GetMapping("/teachplan/{courseId}/tree-nodes")
    public List<TeachplanDto> getTreeNodes(@PathVariable Long courseId) {
        List<TeachplanDto> teachplanDtoList = teachplanService.selectTreeNodes(courseId);
        return teachplanDtoList;
    }

    @ApiOperation("课程计划创建或修改")
    @PostMapping("/teachplan")
    public void saveTeachplan(@RequestBody SaveTeachplanDto teachplan) {
        teachplanService.saveTeachplan(teachplan);
    }

    @ApiOperation("章节删除")
    @DeleteMapping("teachplan/{id}")
    public RestErrorResponse remove(@PathVariable("id") Long teachplanId) {
        RestErrorResponse response = teachplanService.removeTeachplanById(teachplanId);
        return response;
    }

    @ApiOperation("章节上下移动")
    @PostMapping("/teachplan/{moveType}/{teachPlanId}")
    public void move(@PathVariable("moveType") String moveType,
                     @PathVariable("teachPlanId") Long teachPlanId) {
        teachplanService.move(moveType, teachPlanId);
    }
}

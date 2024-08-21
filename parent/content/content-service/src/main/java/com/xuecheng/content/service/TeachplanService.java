package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.base.execption.RestErrorResponse;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;

import java.util.List;

/**
 * <p>
 * 课程计划 服务类
 * </p>
 *
 * @author itcast
 * @since 2024-03-05
 */
public interface TeachplanService extends IService<Teachplan> {

    List<TeachplanDto> selectTreeNodes(Long courseId);

    void saveTeachplan(SaveTeachplanDto teachplan);

    RestErrorResponse removeTeachplanById(Long teachplanId);

    void move(String moveType, Long teachPlanId);

    void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto);
}

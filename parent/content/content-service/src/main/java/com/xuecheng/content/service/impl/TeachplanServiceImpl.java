package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.execption.RestErrorResponse;
import com.xuecheng.base.execption.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanMediaService;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * <p>
 * 课程计划 服务实现类
 * </p>
 *
 * @author itcast
 */
@Slf4j
@Service
public class TeachplanServiceImpl extends ServiceImpl<TeachplanMapper, Teachplan> implements TeachplanService {

    @Autowired
    private TeachplanMediaService teachplanMediaService;

    @Autowired
    private TeachplanMapper teachplanMapper;
    @Autowired
    private TeachplanMediaMapper teachplanMediaMapper;


    @Override
    public List<TeachplanDto> selectTreeNodes(Long courseId) {
        LambdaQueryWrapper<Teachplan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Teachplan::getCourseId, courseId);
        List<Teachplan> teachplans = baseMapper.selectList(wrapper);
        List<TeachplanDto> result = buildTreeTeachplans(teachplans);
        return result;
    }

    @Transactional
    @Override
    public void saveTeachplan(SaveTeachplanDto teachplan) {
        Teachplan teachplan1 = new Teachplan();

        BeanUtils.copyProperties(teachplan, teachplan1);

        if (teachplan.getId() != null) {
            int maxOrder = baseMapper.selectMaxOrderByParentId(teachplan.getParentid(), teachplan.getCourseId());
            teachplan1.setOrderby(maxOrder + 1);
        }

        this.saveOrUpdate(teachplan1);
    }

    @Transactional
    @Override
    public RestErrorResponse removeTeachplanById(Long teachplanId) {
        LambdaQueryWrapper<Teachplan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Teachplan::getParentid, teachplanId);
        int count = baseMapper.selectCount(wrapper);
        if (count > 0) {
            RestErrorResponse restErrorResponse =
                    new RestErrorResponse("课程激活信息还有子级信息，无法操作");
            restErrorResponse.setErrCode("120409");
            return restErrorResponse;
        } else {
            int deleteById = baseMapper.deleteById(teachplanId);
            LambdaQueryWrapper<TeachplanMedia> wrapper1 = new LambdaQueryWrapper<>();
            boolean remove = teachplanMediaService.remove(wrapper1.eq(TeachplanMedia::getTeachplanId, teachplanId));
            if (deleteById <= 0) {
                throw new XueChengPlusException("删除失败");
            } else {
                RestErrorResponse restErrorResponse = new RestErrorResponse("");
                restErrorResponse.setErrCode("200");
                return restErrorResponse;
            }
        }

    }

    @Override
    public void move(String moveType, Long teachPlanId) {
        Teachplan teachplan = baseMapper.selectById(teachPlanId);
        Long parentid = teachplan.getParentid();
        LambdaQueryWrapper<Teachplan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Teachplan::getCourseId, teachplan.getCourseId()).
                ne(Teachplan::getId, teachplan.getId()).
                eq(Teachplan::getParentid, parentid).select(Teachplan::getId, Teachplan::getOrderby);
        if (moveType.equals("movedown")) {
            wrapper.ge(Teachplan::getOrderby, teachplan.getOrderby());
            Teachplan teachplan1 = baseMapper.selectOne(wrapper);
            if (teachplan1 == null) {
                throw new XueChengPlusException("无法下移");
            }
            int temp = teachplan1.getOrderby();
            int temp1 = teachplan.getOrderby();
            if (temp == temp1) {
                temp += 1;
            }

            teachplan.setOrderby(temp);
            teachplan1.setOrderby(temp1);
            baseMapper.updateById(teachplan1);
            baseMapper.updateById(teachplan);
        } else if (moveType.equals("moveup")) {
            wrapper.le(Teachplan::getOrderby, teachplan.getOrderby());
            Teachplan teachplan1 = baseMapper.selectOne(wrapper);
            if (teachplan1 == null) {
                throw new XueChengPlusException("无法上移");
            }
            teachplan.setOrderby(teachplan1.getOrderby());
            teachplan1.setOrderby(teachplan.getOrderby());
            baseMapper.updateById(teachplan1);
            baseMapper.updateById(teachplan);
        } else {
            throw new XueChengPlusException("参数异常");
        }
    }

    @Override
    @Transactional
    public void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto) {

        teachplanMediaMapper.delete(
                new LambdaUpdateWrapper<TeachplanMedia>().
                        eq(TeachplanMedia::getTeachplanId, bindTeachplanMediaDto.getTeachplanId()));
        Teachplan teachplan = teachplanMapper.selectById(bindTeachplanMediaDto.getTeachplanId());
        if (teachplan==null){
            XueChengPlusException.cast("课程计划不存在");
        }
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        BeanUtils.copyProperties(bindTeachplanMediaDto, teachplanMedia);
        teachplanMedia.setCourseId(teachplan.getCourseId());
        teachplanMedia.setMediaFilename(bindTeachplanMediaDto.getFileName());
        teachplanMediaMapper.insert(teachplanMedia);

    }

    private List<TeachplanDto> buildTreeTeachplans(List<Teachplan> teachplans) {
        List<TeachplanDto> teachplanDtoList = new ArrayList<>();
        for (Teachplan teachplan : teachplans) {
            if (teachplan.getParentid().intValue() == 0) {
                TeachplanDto teachplanDto = buildTreeTeachplanTrace(teachplans, teachplan);
                teachplanDtoList.add(teachplanDto);
            }
        }
        Collections.sort(teachplanDtoList, new Comparator<TeachplanDto>() {
            @Override
            public int compare(TeachplanDto o1, TeachplanDto o2) {
                return o1.getOrderby() - o2.getOrderby();
            }
        });
        return teachplanDtoList;
    }

    private TeachplanDto buildTreeTeachplanTrace(List<Teachplan> teachplans, Teachplan teachplan) {
        TeachplanDto teachplanDto = new TeachplanDto();
        TeachplanMedia teachplanMedia = this.teachplanMediaService.getById(teachplan.getId());
        teachplanDto.setTeachplanMedia(teachplanMedia);
        BeanUtils.copyProperties(teachplan, teachplanDto);
        teachplanDto.setTeachPlanTreeNodes(new ArrayList<>());
        for (Teachplan teachplan1 : teachplans) {
            if (teachplan1.getParentid().equals(teachplan.getId())) {
                teachplanDto.getTeachPlanTreeNodes().add(buildTreeTeachplanTrace(teachplans, teachplan1));
            }
        }
        Collections.sort(teachplanDto.getTeachPlanTreeNodes(), new Comparator<TeachplanDto>() {
            @Override
            public int compare(TeachplanDto o1, TeachplanDto o2) {
                return o1.getOrderby() - o2.getOrderby();
            }
        });
        return teachplanDto;
    }
}

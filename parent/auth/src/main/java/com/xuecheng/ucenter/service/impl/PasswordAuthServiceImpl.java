package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {

    @Autowired
    private XcUserMapper userMapper;

    @Autowired
    private CheckCodeClient checkCodeClient;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        //校验验证码
        String checkcode = authParamsDto.getCheckcode();
        String checkcodekey = authParamsDto.getCheckcodekey();
//        if (StringUtils.isEmpty(checkcodekey)||StringUtils.isEmpty(checkcode)){
//            throw new RuntimeException("请输入验证码");
//        }
//        Boolean verify = checkCodeClient.verify(checkcodekey, checkcode);
//        if (verify==null||!verify){
//            throw new RuntimeException("验证码出入错误");
//        }
        //获取用户名查数据库
        String userName=authParamsDto.getUsername();
        LambdaUpdateWrapper<XcUser> wrapper = new LambdaUpdateWrapper<XcUser>()
                .eq(!StringUtils.isEmpty(userName), XcUser::getUsername, userName);
        XcUser xcUser = userMapper.selectOne(wrapper);
        if (xcUser==null){
          throw new RuntimeException("账号或密码错误");
        }
        String password = xcUser.getPassword();
        String passwordForm = authParamsDto.getPassword();
        boolean matches = passwordEncoder.matches(passwordForm, password);
        if (!matches){
            throw new RuntimeException("账号或密码错误");
        }
        xcUser.setPassword(null);
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser,xcUserExt);
        return xcUserExt;
    }
}

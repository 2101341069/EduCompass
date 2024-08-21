package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xuecheng.ucenter.mapper.XcMenuMapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcMenu;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class UserServiceImpl implements UserDetailsService {

    @Autowired
    private XcUserMapper userMapper;

    @Resource
    private XcMenuMapper xcMenuMapper;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * @param s JSON格式的用户信息
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {

        AuthParamsDto authParamsDto = null;

        try {
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        } catch (Exception e) {
            throw new RuntimeException("请求认证参数不符合");
        }

        //认证类型
        String authType = authParamsDto.getAuthType();

        String beanName = authType + "_authservice";

        AuthService authService = applicationContext.getBean(beanName, AuthService.class);
        XcUserExt userExt = authService.execute(authParamsDto);
        UserDetails userDetails = getUserPrincipal(userExt);
        return userDetails;
    }

    private UserDetails getUserPrincipal(XcUserExt user) {
        String[] authorities = {};
        List<XcMenu> xcMenus = xcMenuMapper.selectPermissionByUserId(user.getId());
        if (xcMenus != null && xcMenus.size() > 0) {
            authorities = xcMenus.stream().map(XcMenu::getCode).collect(Collectors.toList()).toArray(new String[0]);
        }
        user.setPermissions(null);
        user.setPassword(null);
        String userJSON = JSON.toJSONString(user);
        user.setPassword(null);
        UserDetails userDetails = User.withUsername(userJSON).password("").authorities(authorities).build();
        return userDetails;
    }
}

package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.AuthService;
import com.xuecheng.ucenter.service.WxAuthService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 微信扫描验证
 */
@Service("wx_authservice")
public class WxAuthServiceImpl implements AuthService, WxAuthService {


    @Autowired
    private XcUserMapper xcUserMapper;

    @Autowired
    private XcUserRoleMapper xcUserRoleMapper;

    @Autowired
    private WxAuthServiceImpl wxAuthService;
    @Autowired
    RestTemplate restTemplate;

    //    @Value("${weixin.appid}")
    String appid = "1234";
    //    @Value("${weixin.secret}")
    String secret = "1245";

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        String userName=authParamsDto.getUsername();
        LambdaUpdateWrapper<XcUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(XcUser::getUsername,userName);
        XcUser xcUser = xcUserMapper.selectOne(wrapper);
        if (xcUser==null){
            throw new RuntimeException("用户不存在");
        }
        XcUserExt userExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser,userExt);
        return userExt;
    }

    @Override
    public XcUser wxAuth(String code) {
        Map<String, String> access_token_map = getAccess_token(code);
        String accessToken = access_token_map.get("access_token_map");
        String openId = access_token_map.get("openid");
        Map<String, String> userInfo = getUserInfo(accessToken, openId);
        XcUser xcUser = wxAuthService.addWxUser(userInfo);
        return xcUser;
    }

    @Transactional
    public XcUser addWxUser(Map<String,String> userInfo_map){
        String unionid =  userInfo_map.get("unionid");
        String nickName=  userInfo_map.get("nickName");
        LambdaUpdateWrapper<XcUser> wrapper = new LambdaUpdateWrapper<XcUser>().eq(XcUser::getWxUnionid, unionid);
        XcUser xcUser = xcUserMapper.selectOne(wrapper);
        if (xcUser!=null){
            return xcUser;
        }

        //想用户表新增记录
        xcUser=new XcUser();
        String userId=UUID.randomUUID().toString();
        xcUser.setId(userId);
        xcUser.setUsername(unionid);
        xcUser.setPassword(unionid);
        xcUser.setWxUnionid(unionid);
        xcUser.setNickname(nickName);
        xcUser.setName(nickName);
        xcUser.setStatus("1");
        xcUser.setUtype("101001");
        xcUser.setCreateTime(LocalDateTime.now());

        //向角色关系表新增
        XcUserRole xcUserRole=new XcUserRole();
        xcUserRole.setId(UUID.randomUUID().toString());
        xcUserRole.setUserId(userId);
        xcUserRole.setRoleId("17");
        xcUserRole.setCreateTime(LocalDateTime.now());

        xcUserRoleMapper.insert(xcUserRole);
        return xcUser;

    }

    /**
     * https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code
     *
     * @param code
     * @return {
     * "access_token":"ACCESS_TOKEN",
     * "expires_in":7200,
     * "refresh_token":"REFRESH_TOKEN",
     * "openid":"OPENID",
     * "scope":"SCOPE",
     * "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"
     * }
     */

    private Map<String, String> getAccess_token(String code) {
        String url_template = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
        String url = String.format(url_template, appid, secret, code);
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, null, String.class);
        //获取相应结果
        String body =new String( exchange.getBody().getBytes(StandardCharsets.ISO_8859_1),StandardCharsets.UTF_8);
        Map<String, String> map = JSON.parseObject(body, Map.class);
        return map;
    }

    /**
     * https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID
     * @param access_token
     * @param openId
     * @return
     */
    private Map<String,String> getUserInfo(String access_token,String openId){
        String url_template="https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
        String url = String.format(url_template, access_token, openId);
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
        String body =new String( exchange.getBody().getBytes(StandardCharsets.ISO_8859_1),StandardCharsets.UTF_8);
        Map<String, String> map = JSON.parseObject(body, Map.class);
        return map;
    }
}

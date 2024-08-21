package com.xuecheng.auth.config;

import org.apache.tomcat.util.digester.ObjectCreateRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

@EnableAuthorizationServer
public class AuthorizationServerDemo extends AuthorizationServerConfigurerAdapter {
    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
            security.tokenKeyAccess("permitAll()").checkTokenAccess("permitAll()").allowFormAuthenticationForClients();
    }

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AuthorizationServerTokenServices authorizationServerTokenServices;
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {

        //"authorization_code", "password","client_credentials","implicit","refresh_token"
            clients.inMemory().withClient("XcWebApp")
                    .secret(new BCryptPasswordEncoder().encode("1234"))
                    .resourceIds("xuecheng-plus")
                    .authorizedGrantTypes("authorization_code","password","client_credentials","implicit","refresh_token")
                    .scopes("all")
                    .autoApprove(false).redirectUris("http://www.51xuecheng.cn");
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
            endpoints.authenticationManager(authenticationManager).
                    tokenServices(authorizationServerTokenServices).
                    allowedTokenEndpointRequestMethods(HttpMethod.POST);
    }
}

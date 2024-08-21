package com.xuecheng.orders.api;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeCreateRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.xuecheng.orders.config.AlipayConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 扫码支付测试
 */
@Controller
public class PayTestController {


    @Value("${pay.APP_ID}")
    private String APP_ID;
    @Value("${pay.ALIPAY_PUBLIC_KEY}")
    private String ALIPAY_PUBLIC_KEY ;
    @Value("${pay.APP_PRIVITE_KEY}")
    private String APP_PRIVITE_KEY ;
    //下单接口测试
    @RequestMapping("/alipaytest")
    public void doPost(HttpServletRequest httpRequest,
                       HttpServletResponse httpResponse) throws ServletException, IOException, AlipayApiException {

        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVITE_KEY, AlipayConfig.FORMAT, AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE);
        AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();//创建API对应的request
//        alipayRequest.setReturnUrl("http://domain.com/CallBack/return_url.jsp");
//        alipayRequest.setNotifyUrl("http://tjxt-user-t.itheima.net/xuecheng/orders/paynotify");//在公共参数中设置回跳和通知地址
        alipayRequest.setBizContent("{" +
                "    \"out_trade_no\":\"202303200101020012\"," +
                "    \"total_amount\":0.1," +
                "    \"subject\":\"Iphone14 16G\"," +
                "    \"product_code\":\"QUICK_WAP_WAY\"" +
                "  }");//填充业务参数
        String form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        httpResponse.setContentType("text/html;charset=" + AlipayConfig.CHARSET);
        httpResponse.getWriter().write(form);//直接将完整的表单html输出到页面
        httpResponse.getWriter().flush();

//        String APP_ID = "线上应用同步到沙箱后的APPID，该APPID跟线上应用一致";
//        String APP_PRIVATE_KEY = "沙箱分配的默认应用私钥";
//        String ALIPAY_PUBLIC_KEY = "沙箱分配的默认支付宝公钥";
//        String BUYER_ID = "建议使用开发平台默认分配的沙箱账号下面的买家信息";
//        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi-sandbox.dl.alipaydev.com/gateway.do",APP_ID,APP_PRIVATE_KEY,"json","GBK",ALIPAY_PUBLIC_KEY,"RSA2");
//        AlipayTradeCreateRequest request = new AlipayTradeCreateRequest();
//        request.setNotifyUrl("");
//        JSONObject bizContent = new JSONObject();
//        bizContent.put("out_trade_no", "20210817010101003X02");
//        bizContent.put("total_amount", 0.01);
//        bizContent.put("subject", "测试商品");
//        bizContent.put("buyer_id", BUYER_ID);
//        bizContent.put("timeout_express", "10m");
//        bizContent.put("product_code", "JSAPI_PAY");
//        request.setBizContent(bizContent.toString());
    }

}

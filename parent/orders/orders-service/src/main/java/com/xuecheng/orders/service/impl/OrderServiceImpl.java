package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xuecheng.base.execption.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.config.PayNotifyConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OrderService orderService;

    @Autowired
    private MqMessageService mqMessageService;
    @Value("${pay.APP_ID}")
    private String APP_ID;
    @Value("${pay.ALIPAY_PUBLIC_KEY}")
    private String ALIPAY_PUBLIC_KEY;
    @Value("${pay.APP_PRIVITE_KEY}")
    private String APP_PRIVITE_KEY;
    @Value("${pay.qrcodeurl}")
    private String qrcodeurl;
    @Resource
    private XcOrdersMapper ordersMapper;

    @Resource
    private XcOrdersGoodsMapper ordersGoodsMapper;


    @Resource
    private XcPayRecordMapper payRecordMapper;

    @Transactional
    @Override
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {

        XcOrders xcOrders = saveXcOrders(userId, addOrderDto);
        XcPayRecord payRecord = createPayRecord(xcOrders);
        Long payNo = payRecord.getPayNo();
        QRCodeUtil qrCodeUtil = new QRCodeUtil();
        //图片base地址
        String qrCode = null;
        try {
            String url = String.format(qrcodeurl, payNo);
            qrCodeUtil.createQRCode(url, 200, 200);
        } catch (IOException e) {
            XueChengPlusException.cast("生成二维码出错");
        }
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        payRecordDto.setQrcode(qrCode);
        return payRecordDto;
    }

    @Override
    public XcPayRecord getPayRecordByPayno(String payNo) {
        LambdaUpdateWrapper<XcPayRecord> wrapper = new LambdaUpdateWrapper<>();
        XcPayRecord xcPayRecord = payRecordMapper.selectOne(wrapper);
        return xcPayRecord;
    }

    @Override
    public PayRecordDto queryPayResult(String payNo) {
        PayStatusDto payStatusDto = queryPayResultFromAlipay(payNo);
        orderService.saveAliPayStatus(payStatusDto);
        XcPayRecord payRecordByPayno = getPayRecordByPayno(payNo);

        return (PayRecordDto) payRecordByPayno;
    }

    /**
     * 修改订单表的状态和支付表的状态
     *
     * @param payStatusDto 支付宝查询支付结果
     */
    public void saveAliPayStatus(PayStatusDto payStatusDto) {
        String payNo = payStatusDto.getTrade_no();//支付记录号
        XcPayRecord payRecordByPayno = getPayRecordByPayno(payNo);
        if (payRecordByPayno == null) {
            XueChengPlusException.cast("找不到相关的支付记录");
        }
        Long orderId = payRecordByPayno.getOrderId();
        XcOrders xcOrders = ordersMapper.selectById(orderId);
        if (orderId == null) {
            XueChengPlusException.cast("找不到相关的订单");
        }
        String statusFormDb = payRecordByPayno.getStatus();
        if ("601002".equals(statusFormDb)) {
            return;
        }
        String trade_status = payStatusDto.getTrade_status();
        if ("TRADE_SUCCESS".equals(trade_status)) {
            payRecordByPayno.setStatus("601002");
            payRecordByPayno.setOutPayNo(payStatusDto.getTrade_no());
            payRecordByPayno.setOutPayChannel("Alipay");
            payRecordByPayno.setPaySuccessTime(LocalDateTime.now());
            payRecordMapper.updateById(payRecordByPayno);
            ordersMapper.update(null,
                    new LambdaUpdateWrapper<XcOrders>().set(XcOrders::getStatus, "60002").
                            eq(XcOrders::getId, xcOrders.getId()));
            MqMessage message = mqMessageService.addMessage("payresult_notify", xcOrders.getOutBusinessId(),
                    xcOrders.getOrderType(), null);
            orderService.notifPayResult(message);
        }
    }

    @Transactional
    @Override
    public void notifPayResult(MqMessage mqMessage) {

        String jsonString = JSON.toJSONString(mqMessage);
        //持久化消息
        Message message = MessageBuilder.withBody(jsonString.getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT).build();


        Long id = mqMessage.getId();
        //全局id
        CorrelationData correlationData = new CorrelationData(id.toString());
        correlationData.getFuture().addCallback(reslut -> {
            if (reslut.isAck()) {
                //消息发送成功
                log.debug("发送消息成功：{}",jsonString);
                //将消息从数据库mq_message表
                mqMessageService.removeById(id);
            }else {

            }
        }, ex -> {
            //发生异常
        });
        //发消息
        rabbitTemplate.convertAndSend(PayNotifyConfig.PAYNOTIFY_EXCHANGE_FANOUT,"",message,correlationData);
    }

    /**
     * 请求支付宝查询支付结果
     *
     * @param payNo 支付订单号
     * @return
     */
    public PayStatusDto queryPayResultFromAlipay(String payNo) {
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVITE_KEY, AlipayConfig.FORMAT,
                AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizCotnent = new JSONObject();
        bizCotnent.put("out_trade_no", payNo);
        request.setBizContent(bizCotnent.toString());
        String body = "";
        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            body = response.getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
            XueChengPlusException.cast("请求查询支付信息结果异常");
        }
        Map<String, String> map = JSON.parseObject(body, Map.class);
        PayStatusDto payStatusDto = new PayStatusDto();
        payStatusDto.setOut_trade_no(payNo);
        payStatusDto.setTrade_no(map.get("trade_no"));
        payStatusDto.setTrade_status(map.get("trade_status"));
        payStatusDto.setApp_id(APP_ID);
        payStatusDto.setTotal_amount(map.get("total_amount"));
        return payStatusDto;
    }

    /**
     * 插入支付记录表
     *
     * @param orders 支付订单
     * @return
     */
    public XcPayRecord createPayRecord(XcOrders orders) {
        Long ordersId = orders.getId();
        XcOrders xcOrders = ordersMapper.selectById(ordersId);
        if (xcOrders == null) {
            XueChengPlusException.cast("订单不存在");
        }
        LambdaUpdateWrapper<XcPayRecord> wrapper = new LambdaUpdateWrapper<XcPayRecord>()
                .eq(XcPayRecord::getOrderId, ordersId);
        List<XcPayRecord> xcPayRecords = payRecordMapper.selectList(wrapper);
        if (xcPayRecords.size() > 0) {
            for (XcPayRecord xcPayRecord : xcPayRecords) {
                if ("600001".equals(xcPayRecord.getStatus())) {
                    return xcPayRecord;
                }
            }
        }
        String status = xcOrders.getStatus();
        if ("600002".equals(status)) {
            XueChengPlusException.cast("此订单已经支付");
        }
        XcPayRecord xcPayRecord = new XcPayRecord();
        xcPayRecord.setPayNo(IdWorkerUtils.getInstance().nextId());//雪花算法生成本系统id
        xcPayRecord.setOrderId(xcOrders.getId());
        xcPayRecord.setTotalPrice(xcOrders.getTotalPrice());
        xcPayRecord.setCurrency("CNY");
        xcPayRecord.setStatus("601001");
        xcPayRecord.setUserId(xcOrders.getUserId());
        xcPayRecord.setCreateDate(LocalDateTime.now());
        xcPayRecord.setOrderName(orders.getOrderName());
        int insert = payRecordMapper.insert(xcPayRecord);
        if (insert <= 0) {
            XueChengPlusException.cast("插入订单记录失败");
        }
        return xcPayRecord;
    }


    /**
     * 插入订单表
     *
     * @param userId      用户id
     * @param addOrderDto 添加用户信息
     * @return
     */
    public XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto) {
        XcOrders xcOrders = getOrderByBusinessId(addOrderDto.getOutBusinessId());
        if (xcOrders != null) {
            return xcOrders;
        }
        //插入主表
        xcOrders = new XcOrders();
        //雪花算法生成主键
        xcOrders.setId(IdWorkerUtils.getInstance().nextId());

        xcOrders.setTotalPrice(addOrderDto.getTotalPrice());
        xcOrders.setCreateDate(LocalDateTime.now());
        xcOrders.setStatus("60001");
        xcOrders.setUserId(userId);
        xcOrders.setOrderType("60201");
        xcOrders.setOrderName(addOrderDto.getOrderName());
        xcOrders.setOrderDescrip(addOrderDto.getOrderDescrip());
        xcOrders.setOrderDetail(addOrderDto.getOrderDetail());
        xcOrders.setOutBusinessId(addOrderDto.getOutBusinessId());//如果是选课，这里则是选课表的id
        int insert = ordersMapper.insert(xcOrders);
        if (insert <= 0) {
            XueChengPlusException.cast("添加订单失败");
        }
        Long ordersId = xcOrders.getId();
        //插入订单明细表
        String orderDetail = addOrderDto.getOrderDetail();
        List<XcOrdersGoods> xcOrdersGoods = JSON.parseArray(orderDetail, XcOrdersGoods.class);
        xcOrdersGoods.forEach((goods) -> {
            goods.setOrderId(ordersId);
            int insert1 = ordersGoodsMapper.insert(goods);
            if (insert1 <= 0) {
                XueChengPlusException.cast("插入订单明细表失败");
            }

        });
        return xcOrders;
    }

    /**
     * 根据业务id查询
     *
     * @param businessId 业务id
     * @return
     */
    public XcOrders getOrderByBusinessId(String businessId) {
        XcOrders xcOrders = ordersMapper.selectOne(new LambdaUpdateWrapper<XcOrders>().eq(XcOrders::getOutBusinessId, businessId));

        return xcOrders;
    }
}

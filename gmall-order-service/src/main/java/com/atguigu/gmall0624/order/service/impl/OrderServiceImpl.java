package com.atguigu.gmall0624.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall0624.bean.OrderDetail;
import com.atguigu.gmall0624.bean.OrderInfo;
import com.atguigu.gmall0624.bean.enums.OrderStatus;
import com.atguigu.gmall0624.bean.enums.ProcessStatus;
import com.atguigu.gmall0624.order.mapper.OrderDetailMapper;
import com.atguigu.gmall0624.order.mapper.OrderInfoMapper;
import com.atguigu.gmall0624.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Service
public class OrderServiceImpl implements OrderService{

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Override
    @Transactional
    public String saveOrder(OrderInfo orderInfo) {
        // 两张表 orderInfo ,orderDetail
        // 总金额，订单状态，[用户Id]，第三方交易变化，创建时间，过期时间，进程状态
        orderInfo.sumTotalAmount();
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        // 支付使用
        String outTradeNo = "ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setCreateTime(new Date());
        // 过期时间：+1天
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);

        orderInfo.setExpireTime(calendar.getTime());

        orderInfo.setProcessStatus(ProcessStatus.UNPAID);

        orderInfoMapper.insertSelective(orderInfo);

        // 保存订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (orderDetailList!=null && orderDetailList.size()>0){
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetail.setId(null);
                orderDetail.setOrderId(orderInfo.getId());
                orderDetailMapper.insertSelective(orderDetail);
            }
        }
        return orderInfo.getId();
    }
}

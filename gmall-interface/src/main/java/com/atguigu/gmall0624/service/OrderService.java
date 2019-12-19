package com.atguigu.gmall0624.service;

import com.atguigu.gmall0624.bean.OrderInfo;

public interface OrderService {
    /**
     * 保存订单
     * @param orderInfo
     * @return
     */
    String saveOrder(OrderInfo orderInfo);
}

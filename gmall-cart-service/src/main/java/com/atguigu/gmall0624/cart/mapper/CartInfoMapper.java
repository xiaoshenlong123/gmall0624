package com.atguigu.gmall0624.cart.mapper;

import com.atguigu.gmall0624.bean.CartInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface CartInfoMapper extends Mapper<CartInfo> {
    //根据用户id查询购物车列表
    List<CartInfo> selectCartListWithCurPrice(String userId);
}

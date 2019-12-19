package com.atguigu.gmall0624.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class CartInfo implements Serializable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column
    String id;
    @Column
    String userId;
    @Column
    String skuId;
    @Column
    BigDecimal cartPrice;
    @Column
    Integer skuNum;
    @Column
    String imgUrl;
    @Column
    String skuName;
    @Column
    String isChecked="1";

    // 实时价格 添加购物车的时候1999 ，元旦 购物车的价格 1899 | skuPrice=skuInfo.price
    @Transient
    BigDecimal skuPrice;


}

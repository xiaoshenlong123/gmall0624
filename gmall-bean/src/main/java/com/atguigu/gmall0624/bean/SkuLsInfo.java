package com.atguigu.gmall0624.bean;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuLsInfo implements Serializable{
    // skuId
    String id;
    // 价格
    BigDecimal price;
    // 名称
    String skuName;
    // 分类Id
    String catalog3Id;
    // 默认图片
    String skuDefaultImg;

    // 热度排名
    Long hotScore=0L;
    // 平台属性值Id 集合 {valueId}
    List<SkuLsAttrValue> skuAttrValueList;

    // 以上来自于skuInfo的属性值！ 想做一个属性拷贝！
}


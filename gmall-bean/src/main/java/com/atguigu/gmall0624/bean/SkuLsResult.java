package com.atguigu.gmall0624.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SkuLsResult implements Serializable {

    // 页面显示商品信息
    List<SkuLsInfo> skuLsInfoList;
    // 查询出来的总条数
    long total;
    // 总页数
    long totalPages;
    // 平台属性值Id 集合
    // 通过两张表多表关联就可以查询平台属性，平台属性值！
    List<String> attrValueIdList;

}

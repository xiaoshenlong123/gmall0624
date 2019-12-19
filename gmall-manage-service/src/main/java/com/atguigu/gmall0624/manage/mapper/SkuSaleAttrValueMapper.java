package com.atguigu.gmall0624.manage.mapper;

import com.atguigu.gmall0624.bean.SkuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {
    /**
     * 根据spuId查询sku与商品销售属性中间表的集合
     * @param spuId
     * @return
     */
    List<SkuSaleAttrValue> selectSkuSaleAttrValueListBySpu(String spuId);
}

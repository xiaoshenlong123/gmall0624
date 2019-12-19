package com.atguigu.gmall0624.manage.mapper;

import com.atguigu.gmall0624.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {
    //根据三级分类id查询平台属性以及平台属性值
    List<BaseAttrInfo> selectBaseAttrInfoListByCatalog3Id(String catalog3Id);
    //根据平台属性值Id集合查询平台属性和平台属性值集合
    List<BaseAttrInfo> selectAttrInfoListByIds(@Param("valueIds") String valueIds);
}

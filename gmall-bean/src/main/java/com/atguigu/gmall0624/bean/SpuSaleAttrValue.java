package com.atguigu.gmall0624.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;

@Data
public class SpuSaleAttrValue implements Serializable{
    @Id
    @Column
    private String id ;

    @Column
    private String spuId;

    @Column
    private String saleAttrId;

    @Column
    private String saleAttrValueName;

    // 当前销售属性值是否被选中！
    @Transient
    private String isChecked;

}

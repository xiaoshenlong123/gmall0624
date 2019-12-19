package com.atguigu.gmall0624.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;

@Data
public class BaseAttrValue implements Serializable{
    @Id
    @Column
    private String id;
    @Column
    private String valueName;

    // BaseAttrInfo.id
    @Column
    private String attrId;

    //声明一个属性来存储最新的urlParam
    @Transient
    private String urlParam;

}

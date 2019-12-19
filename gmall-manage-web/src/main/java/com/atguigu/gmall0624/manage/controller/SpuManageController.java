package com.atguigu.gmall0624.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0624.bean.BaseSaleAttr;
import com.atguigu.gmall0624.bean.SpuInfo;
import com.atguigu.gmall0624.service.ManageService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
public class SpuManageController {

    @Reference
    private ManageService manageService;

    // http://localhost:8082/spuList?catalog3Id=61
    // 根据三级分类id查询
    @RequestMapping("spuList")
    public List<SpuInfo> getSpuList(SpuInfo spuInfo){
        return manageService.getSpuList(spuInfo);
    }

    // http://localhost:8082/baseSaleAttrList
    // 查询所有销售属性
    @RequestMapping("baseSaleAttrList")
    public List<BaseSaleAttr> getBaseSaleAttrList(){
        return manageService.getBaseSaleAttrList();
    }

    // http://localhost:8082/saveSpuInfo
    // 保存商品属性SPU（保存SpuInfo数据）
    @RequestMapping("saveSpuInfo")
    public void saveSpuInfo(@RequestBody SpuInfo spuInfo){
        manageService.saveSpuInfo(spuInfo);
    }
}

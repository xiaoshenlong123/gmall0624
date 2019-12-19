package com.atguigu.gmall0624.manage.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0624.bean.*;
import com.atguigu.gmall0624.service.ManageService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
public class ManageController {

    @Reference
    private ManageService manageService;

    //  http://localhost:8082/getCatalog1
    // 获取所有一级分类集合
    @RequestMapping("getCatalog1")
    public List<BaseCatalog1> getCatalog1(){
        return manageService.getCatalog1();
    }

    // http://localhost:8082/getCatalog2?catalog1Id=2
    // 根据一级分类id获取二级分类集合
    @RequestMapping("getCatalog2")
    public List<BaseCatalog2> getCatalog2(BaseCatalog2 baseCatalog2){
        return manageService.getCatalog2(baseCatalog2);
    }

    // http://localhost:8082/getCatalog3?catalog2Id=13
    // 根据二级分类id获取三级分类集合
    @RequestMapping("getCatalog3")
    public List<BaseCatalog3> getCatalog3(BaseCatalog3 baseCatalog3){
        return manageService.getCatalog3(baseCatalog3);
    }


    // http://localhost:8082/attrInfoList?catalog3Id=61
    // 根据三级分类id获取平台属性
    @RequestMapping("attrInfoList")
    public List<BaseAttrInfo> getAttrInfo(BaseAttrInfo baseAttrInfo,String catalog3Id){
        return manageService.getAttrInfoList(catalog3Id);
    }

    // http://localhost:8082/saveAttrInfo
    // 保存平台属性
    @RequestMapping("saveAttrInfo")
    public void saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        manageService.saveAttrInfo(baseAttrInfo);
    }

    // http://localhost:8082/getAttrValueList?attrId=100
    // 根据平台属性id查询平台属性值
    @RequestMapping("getAttrValueList")
    public List<BaseAttrValue> getAttrValueList(String attrId){
        return manageService.getAttrValueList(attrId);
    }

}

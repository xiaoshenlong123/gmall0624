package com.atguigu.gmall0624.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0624.bean.*;
import com.atguigu.gmall0624.service.ListService;
import com.atguigu.gmall0624.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;

    @RequestMapping("list.html")
    public String getList(SkuLsParams skuLsParams, HttpServletRequest request){
        // 设置分页数据：每页显示的条数
        skuLsParams.setPageSize(3);

        SkuLsResult skuLsResult = listService.search(skuLsParams);
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();
        //获取到平台属性Id集合{171,81,120,167,82,83}
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        //调用方法将Id集合传入
        List<BaseAttrInfo> baseAttrInfoList =manageService.getAttrList(attrValueIdList);

        //如何保存用户查询的条件
        //如果当前对象skuLsParams的三级分类id不为null，则证明用户走的是三级分类id检索，如果keyword不为空，则证明用户走的是关键字全文检索
        String urlParam = makeUrlParam(skuLsParams);

        //声明一个面包屑集合
        ArrayList<BaseAttrValue> baseAttrValueArrayList = new ArrayList<>();
        //点击平台属性值过滤时，平台属性消失 ----itar,iter,itco?
        if(baseAttrInfoList!=null && baseAttrInfoList.size()>0){
            for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {
                BaseAttrInfo baseAttrInfo = iterator.next();
                //得到平台属性值集合
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                //循环遍历
                //说明用户点击平台属性值过滤
                if(skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
                    //url后面的valueId
                    for (String valueId : skuLsParams.getValueId()) {
                        //从mysql中查询出来的valueId
                        for (BaseAttrValue baseAttrValue : attrValueList) {
                            //判断valueId是否相等
                            if(valueId.equals(baseAttrValue.getId())){
                                //移除当前平台属性值数据
                                iterator.remove();

                                //声明一个平台属性值对象
                                BaseAttrValue baseAttrValueed = new BaseAttrValue();
                                //组成面包屑：平台属性值:平台属性值名称
                                baseAttrValueed.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());

                                //制作新的urlParam
                                String newUrlParam = makeUrlParam(skuLsParams, valueId);
                                //将最新的参数赋给当前的变量
                                baseAttrValueed.setUrlParam(newUrlParam);

                                //将面包屑封装进面包屑集合中
                                baseAttrValueArrayList.add(baseAttrValueed);
                            }
                        }
                    }
                }
            }
        }
        // 设置分页
        request.setAttribute("totalPages",skuLsResult.getTotalPages());
        request.setAttribute("pageNo",skuLsParams.getPageNo());

        // 面包屑显示
        request.setAttribute("baseAttrValueArrayList",baseAttrValueArrayList);
        //保存一个关键字
        request.setAttribute("keyword",skuLsParams.getKeyword());
        //返回每次拼接的url
        request.setAttribute("urlParam",urlParam);
        //将根据平台属性值id集合查询出来的平台属性名称和平台属性值名称的集合保存至作用域
        request.setAttribute("baseAttrInfoList",baseAttrInfoList);
        //存储商品数据
        request.setAttribute("skuLsInfoList",skuLsInfoList);
        return "list";
    }

    //制作参数

    /**
     *
     * @param skuLsParams 表示用户url中输入的查询参数条件
     * @param excludeValueIds 表示用户点击面包屑时传递过来的平台属性值id
     * @return
     */
    private String makeUrlParam(SkuLsParams skuLsParams,String ... excludeValueIds) {
        String urlParam = "";
        //用户走的是全文检索
        //http://list.gmall.com/list.html?keyword=小米
        if(skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            //urlParam = keyword=小米
            //'list.html?'+${urlParam} == http://list.gmall.com/list.html?keyword=小米
            urlParam+="keyword="+skuLsParams.getKeyword();
        }
        //用户走的是三级分类id
        if(skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            urlParam+="catalog3Id="+skuLsParams.getCatalog3Id();
        }

        //判断是否有平台属性值id
        //用户通过三级分类id查询，第二步平台属性值过滤
        if(skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
            //循环遍历
            for (String valueId : skuLsParams.getValueId()) {
                //用户点击面包屑时的平台属性值id
                if(excludeValueIds!=null && excludeValueIds.length>0){
                    String excludeValueId = excludeValueIds[0];
                    if(valueId.equals(excludeValueId)){
                        //使用什么停止当前次数的拼接！
                        continue;
                    }
                }
                //有平台属性值id时要拼接 &
                urlParam+="&valueId="+valueId;
            }
        }
        return urlParam;
    }
}

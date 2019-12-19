package com.atguigu.gmall0624.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0624.bean.UserInfo;
import com.atguigu.gmall0624.service.UserInfoService;
import com.atguigu.gmall0624.passport.config.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    private UserInfoService userInfoService;

    @Value("${token.key}")
    private String key ;

    // 表示登录的首页
    // http://localhost:8087/index?originUrl=https%3A%2F%2Fwww.jd.com%2F
    @RequestMapping("index")
    public String index(HttpServletRequest request){
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);
        return "index";
    }

    // 登录控制器：
    // 登录：userInfo
    @RequestMapping("login")
    @ResponseBody
    public String login(HttpServletRequest request, UserInfo userInfo){
        // 调用服务层方法
        UserInfo info = userInfoService.login(userInfo);

        if (info!=null){
            HashMap<String, Object> map = new HashMap<>();
            map.put("userId",info.getId());
            map.put("nickName",info.getNickName());
            // 服务器的Ip
            String salt = request.getHeader("X-forwarded-for");
            String token = JwtUtil.encode(key, map, salt);
            // 登录成功！
            return token;
        }

        return "fail"; // 登录失败！
    }
    //用户认证中心
    //用户登录认证
    // http://passport.atguigu.com/verify?token=xx&salt=x
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
        /*
           	a.	从url 路径上得到token ，salt
            b.	使用jwt 解密得到用户的数据{map}
            c.	获取map 中的userId 查询缓存
            d.	true:success 	false:fail
         */
        // 需要得到token,salt
        String token = request.getParameter("token");
        String salt = request.getParameter("salt");
        //使用JWT工具类进行解密
        Map<String, Object> map = JwtUtil.decode(token, key, salt);
        //判断map
        if(map!=null&&map.size()>0){
            //获取map中的userId到缓存中查找数据
            String userId = (String) map.get("userId");
            UserInfo userInfo = userInfoService.verify(userId);
            if(userInfo!=null){
                //说明缓存中有数据
                return "success";
            }
        }
        return "fail";
    }
}

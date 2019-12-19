package com.atguigu.gmall0624.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall0624.bean.UserAddress;
import com.atguigu.gmall0624.bean.UserInfo;
import com.atguigu.gmall0624.service.UserInfoService;
import com.atguigu.gmall0624.user.mapper.UserAdressMapper;
import com.atguigu.gmall0624.user.mapper.UserInfoMapper;
import com.atguigu.gmall0624.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;


import java.util.List;

@Service
public class UserInfoServiceImpl implements UserInfoService {

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=7*60*60*24;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAdressMapper userAdressMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<UserInfo> findAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserInfo> findUserInfo(UserInfo userInfo) {
        return null;
    }

    @Override
    public List<UserInfo> findByNickName(String nickName) {
        return null;
    }

    @Override
    public void addUser(UserInfo userInfo) {

    }

    @Override
    public void updUser(UserInfo userInfo) {

    }

    @Override
    public void delUser(UserInfo userInfo) {

    }

    @Override
    public List<UserAddress> finUserAdressListByUserId(String userId) {
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        return userAdressMapper.select(userAddress);
    }

    @Override
    public List<UserAddress> finUserAdressListByUserId(UserAddress userAddress) {
        return userAdressMapper.select(userAddress);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        //SELECT * FROM user_info WHERE loginName = ? and passwd = ?
        String passwd = userInfo.getPasswd();
        //将密码进行加密
        String newPasswd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        //将加密后的密码放入userinfo中
        userInfo.setPasswd(newPasswd);

        //从数据库查询用户是否存在
        UserInfo info = userInfoMapper.selectOne(userInfo);
        if(info!=null){
            //获取Jedis
            Jedis jedis = redisUtil.getJedis();
            //将用户信息存入redis缓存
            //考虑数据存储类型？
            jedis.setex(userKey_prefix+info.getId()+userinfoKey_suffix,userKey_timeOut, JSON.toJSONString(info));
            jedis.close();
            return info;
        }
            return null;
    }

    @Override
    public UserInfo verify(String userId) {
        //获取Jedis
        Jedis jedis = redisUtil.getJedis();
        //通过useId生成key
        String userKey = userKey_prefix+userId+userinfoKey_suffix;
        //从缓存获取数据
        String userJson = jedis.get(userKey);
        if(!StringUtils.isEmpty(userJson)){
            //将userJson转换成对象返回
            UserInfo userInfo = JSON.parseObject(userJson,UserInfo.class);
            return userInfo;
        }
        jedis.close();
        return null;
    }
}

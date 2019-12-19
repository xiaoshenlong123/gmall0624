package com.atguigu.gmall0624.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall0624.bean.CartInfo;
import com.atguigu.gmall0624.bean.SkuInfo;
import com.atguigu.gmall0624.cart.constant.CartConst;
import com.atguigu.gmall0624.cart.mapper.CartInfoMapper;
import com.atguigu.gmall0624.service.CartService;
import com.atguigu.gmall0624.service.ManageService;
import com.atguigu.gmall0624.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Reference
    private ManageService manageService;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        /*
        数据存储：redis + mysql
        1.  购物车中有该商品时，则数量相加
            select * from cartInfo where skuId = ? and userId=?
        2.  购物车中没有该商品时，直接添加
        3.  先mysql 后redis
        */

        //获取jedis
        Jedis jedis = redisUtil.getJedis();
        //确定数据类型 hash 确定 key=user:userId:cart
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;

        //添加之前判断缓存中是否有当前用户购物车的key
        if (!jedis.exists(cartKey)) {
            // 加载数据库的数据到缓存！
            loadCartCache(userId);
        }

        // hash  hset(key,field,value)
        // key = user:userId:cart field = skuId value= cartInfo的字符串

        System.out.println("--------------------------------------------------------");
    /*      CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setUserId(userId);
            select * from cartInfo where skuId = ? and userId=?
            SELECT id,user_id,sku_id,cart_price,sku_num,img_url,sku_name,is_checked FROM cart_info WHERE user_id = ? AND sku_id = ? AND is_checked = ?
            CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfo);

            SELECT id,user_id,sku_id,cart_price,sku_num,img_url,sku_name,is_checked FROM cart_info WHERE ( sku_id = ? and user_id = ? )*/
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("skuId", skuId).andEqualTo("userId", userId);
        CartInfo cartInfoExist = cartInfoMapper.selectOneByExample(example);
        if (cartInfoExist != null) {
            // 购物车中有数据
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() + skuNum);

            // 初始化实时价格
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());

            // 更新数据库
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);

            // 更新缓存
            // jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfoExist));

        } else {
            // 第一次添加购物车
            CartInfo cartInfo1 = new CartInfo();
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);

            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuNum(skuNum);
            cartInfo1.setUserId(userId);
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setSkuId(skuId);

            cartInfoMapper.insertSelective(cartInfo1);
            // 放入缓存！
            // jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfo1));
            cartInfoExist = cartInfo1;
        }
        // 放入缓存
        jedis.hset(cartKey, skuId, JSON.toJSONString(cartInfoExist));
        setCartExpireTime(userId, jedis, cartKey);

        // 关闭：
        jedis.close();

    }

    @Override
    public List<CartInfo> getCartList(String userId) {
         /*
            1.  先走缓存
            2.  如果缓存没有走数据库
            3.  将数据库信息存入缓存
         */
        List<CartInfo> cartInfoList = new ArrayList<>();
        // 缓存如何存储的数据 hash key = user:userId:cart
        // 获取jedis
        Jedis jedis = redisUtil.getJedis();
        // 确定数据类型 hash，确定key = user:userId:cart
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        //根据cartKey获取缓存中所有的数据
        // jedis.hget() 不行 只能获取一条数据！
        // Map<String, String> stringStringMap = jedis.hgetAll(cartKey);
        List<String> stringList = jedis.hvals(cartKey);
        if (stringList != null && stringList.size() > 0) {
            for (String cartJson : stringList) {
                //将cartJson转换成对象
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            //按照商品的更新时间{id}进行排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                //自定义比较器
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    // str1 = "ab" str2 = "ac"
                    return o1.getId().compareTo(o2.getId());
                }
            });
            return cartInfoList;
        } else {
            //如果缓存为空则查询数据库
            // 走数据库！将数据放入缓存
            cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }
    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartInfoNoLoginList, String userId) {
         /*
            1.  获取登录的购物车 {userId}
            2.  登录+未登录开始合并 条件{skuId}
         */
        /*
        demo1:
            登录：
            37 1
            38 1
            未登录：
            37 1
            38 1
            39 1
            合并之后的数据
            37 2
            38 2
            39 1
        demo2:
            未登录：
            37 1
            38 1
            39 1
            40  1
            登录：没有数据
            合并之后的数据
            37 1
            38 1
            39 1
            40  1
          */
        //获取登录的购物车
        //根据用户id获取最新的购物车数据
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        if (cartInfoList != null && cartInfoList.size() > 0) {
            for (CartInfo cartInfo : cartInfoNoLoginList) {
                // 标识是否有相同的商品
                boolean isMatch = false;
                for (CartInfo info : cartInfoList) {
                    //合并条件skuId相等
                    if (cartInfo.getSkuId().equals(info.getSkuId())) {
                        //数量相加
                        info.setSkuNum(info.getSkuNum() + cartInfo.getSkuNum());
                        // 更新数据库
                        cartInfoMapper.updateByPrimaryKeySelective(info);
                        isMatch = true;
                    }
                }
                // 表示没有找到相同的商品
                if (!isMatch) {
                    // 直接插入到数据
                    // 处理39
                    cartInfo.setId(null);
                    cartInfo.setUserId(userId);
                    cartInfoMapper.insertSelective(cartInfo);
                }
            }
        } else {
            // 数据库中根据没有添加过数据 demo2
            for (CartInfo cartInfo : cartInfoNoLoginList) {
                cartInfo.setId(null);
                cartInfo.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfo);
            }
        }
        //返回合并之后的结果
        List<CartInfo> infoList = loadCartCache(userId);
        // 合并之后的数据与未登录购物车数据进行合并 以未登录为基准{数量相加}
        for (CartInfo cartInfo : infoList) {
            // 循环未登录购物车数据
            for (CartInfo info : cartInfoNoLoginList) {
                // 合并条件
                if (info.getSkuId().equals(cartInfo.getSkuId())){
                    // 获取到未登录状态为选中的商品
                    if ("1".equals(info.getIsChecked())){
                        cartInfo.setIsChecked("1");
                        // 自动调用选中方法
                        checkCart(info.getSkuId(),userId,"1");
                    }
                }
            }
        }
        return infoList;
    }

    //删除未登录购物车中的数据
    @Override
    public void deleteCartList(String userTempId) {
        // redis + mysql
        // 先删除mysql ，在删除redis
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId", userTempId);

        cartInfoMapper.deleteByExample(example);

        // 获取Jedis
        Jedis jedis = redisUtil.getJedis();
        // 确定数据类型 hash，确定key = user:userId:cart
        String cartKey = CartConst.USER_KEY_PREFIX + userTempId + CartConst.USER_CART_KEY_SUFFIX;

        // 删除数据
        jedis.del(cartKey);
        // 关闭缓存
        jedis.close();

    }

    //商品选中状态
    @Override
    public void checkCart(String skuId, String userId, String isChecked) {
        //如果同时对缓存和数据库进行了修改的操作，则需要先修改数据库，再删除缓存数据
        //按照缓存管理的原则：避免出现脏数据，先删除缓存，再放入缓存
        //1.修改数据库
        // sql 文： update cartInfo set ischecked = ? where userId = ? and skuId = ?
        // cartInfo 修改的数据 ，example 修改的条件
        CartInfo cartInfoUpd = new CartInfo();
        cartInfoUpd.setIsChecked(isChecked);
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("skuId", skuId).andEqualTo("userId", userId);
        cartInfoMapper.updateByExampleSelective(cartInfoUpd, example);
        //2.删除缓存
        Jedis jedis = redisUtil.getJedis();
        //获取key
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        //删除
        jedis.hdel(cartKey, skuId);
        //将新数据放入缓存
        // select * from cartInfo where userId = ? and skuId = ?
        List<CartInfo> cartInfoList = cartInfoMapper.selectByExample(example);
        if (cartInfoList != null && cartInfoList.size() > 0) {
            CartInfo cartInfoQuery = cartInfoList.get(0);
            // 数据初始化实时价格！
            cartInfoQuery.setSkuPrice(cartInfoQuery.getCartPrice());
            jedis.hset(cartKey, skuId, JSON.toJSONString(cartInfoQuery));
        }
        jedis.close();
    }

    //根据用户id查询数据库中的信息
    private List<CartInfo> loadCartCache(String userId) {
        // 获取jedis
        Jedis jedis = redisUtil.getJedis();

        // 确定数据类型 hash，确定key = user:userId:cart
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

        // SELECT * FROM  cart_info WHERE user_id='b1012f91a9134e989227d2fe5379cefe';
        // 如果缓存失效了，则查询一下最新价格 skuInfo.price
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);

        if (cartInfoList==null || cartInfoList.size()==0){
            return null;
        }
        HashMap<String, String> map = new HashMap<>();
        // 将数据库中的数据放入缓存
        for (CartInfo cartInfo : cartInfoList) {
            // 每个对象都放一次
            // jedis.hset(cartKey,cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
            map.put(cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
        }
        // 一次放入多条数据
        jedis.hmset(cartKey,map);

        jedis.close();

        return cartInfoList;
    }


    //设置缓存过期时间
    private void setCartExpireTime(String userId, Jedis jedis, String cartKey) {
        // 设置过期时间：与用户的过期时间一致！
        // 用key
        String userKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;

        // 获取过期时间
        Long ttl = jedis.ttl(userKey);

        if (!jedis.exists(userKey)){
            jedis.expire(cartKey,30*24*3600);
        }else {
            jedis.expire(cartKey,ttl.intValue());
        }
    }
}

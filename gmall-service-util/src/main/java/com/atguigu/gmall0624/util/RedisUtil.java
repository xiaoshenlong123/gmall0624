package com.atguigu.gmall0624.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtil {
    /**
     * 1.创建连接池
     * 2.初始化连接池
     * 3.从连接池获取Jedis
     */
    //使用Jedis原生的命令
    private JedisPool jedisPool;

    public void initJedisPool(String host,int port,int timeOut){
        //PoolConfig指配置连接池的参数
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        // 达到最大连接数时，需要放入等待队列中！
        jedisPoolConfig.setBlockWhenExhausted(true);
        // 最小剩余数
        jedisPoolConfig.setMinIdle(10);
        // 设置等待时间
        jedisPoolConfig.setMaxWaitMillis(10*1000);
        // 核心数
        jedisPoolConfig.setMaxTotal(200);
        // 获取到连接之后，自检！
        jedisPoolConfig.setTestOnBorrow(true);
        // ,password
        jedisPool = new JedisPool(jedisPoolConfig,host,port,timeOut);

    }
    public Jedis getJedis(){
        Jedis jedis = jedisPool.getResource();
        return jedis;
    }

}

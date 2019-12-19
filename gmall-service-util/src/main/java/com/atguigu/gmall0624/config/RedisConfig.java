package com.atguigu.gmall0624.config;

import com.atguigu.gmall0624.util.RedisUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * spring 如何整合redis！
 * beans.xml
 * <bean id="jedis" class="redis.clients.jedis.Jedis">
 *  <property name="host" value="192.168.67.225"/>
 *  <property name="port" value="6379"/>
 * </bean>
 *
 *  ac a = new ac("beans.xml")
 *  Jedis jedis = a.getBean("jedis");
 *  jedis.set("k1","v1");
 *
 */

@Configuration
public class RedisConfig {
    //实现软编码：@Value表示从application.properties中
    // :disabled表示如果从配置文件中没有找到对应的数据则给一个默认值“disabled”
    @Value("${spring.redis.host:disabled}")
    private String host;

    @Value("${spring.redis.port:0}")
    private int port;

    @Value("${spring.redis.timeOut:10000}")
    private int timeOut;

    //将host，port，timeOut给initJedisPool方法使用
    //@Bean表示一个bean标签，将redistUtil注入到spring容器
     /*
        <bean class="com.atguigu.gmall0624.config.RedistUtil">
        </bean>
     */
    @Bean
    public RedisUtil getRedisUtil(){
        //配置文件中根本没有host
        if("disabled".equals(host)){
            return null;
        }
        RedisUtil redisUtil = new RedisUtil();
        redisUtil.initJedisPool(host,port,timeOut);
        return redisUtil;
    }
}

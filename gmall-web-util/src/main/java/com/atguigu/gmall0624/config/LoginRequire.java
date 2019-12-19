package com.atguigu.gmall0624.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequire {
    // 字段 判断用户是否必须登录 autoRedirect = true 表示必须登录 false ：可以不用登录
    boolean autoRedirect() default true;
}

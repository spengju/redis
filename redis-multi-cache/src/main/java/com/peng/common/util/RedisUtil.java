package com.peng.common.util;

import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @Author: spengju
 * @Slogan: Day day no bug.
 * @Date: 2024/9/30 01:02
 */
public class RedisUtil {

//    private static final RedisTemplate redisTemplate = ApplicationContextHolder.getBean(RedisTemplate.class);
    private static final RedisTemplate redisTemplate = ApplicationContextHolder.getBean("redisTemplate");

    public static void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public static void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public static boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
    }

    public static <T> T get(String key, Class<?> T) {
        return (T) redisTemplate
                .opsForValue().get(key);
    }

    public static String get(String key) {
        return (String) redisTemplate
                .opsForValue().get(key);
    }

    public static Long decr(String key) {
        return redisTemplate
                .opsForValue().decrement(key);
    }

    public static Long decr(String key, long delta) {
        return redisTemplate
                .opsForValue().decrement(key, delta);
    }

    public static Long incr(String key) {
        return redisTemplate
                .opsForValue().increment(key);
    }

    public static Long incr(String key, long delta) {
        return redisTemplate
                .opsForValue().increment(key, delta);
    }

    public static void expire(String key, long time, TimeUnit unit) {
        redisTemplate.expire(key, time, unit);
    }

}


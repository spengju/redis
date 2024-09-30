package com.peng.redission.filter;

import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

/**
 * @Author: spengju
 * @Slogan: Day day no bug.
 * @Date: 2024/9/29 19:54
 */
public class RedissonBloomFilter {

    public static void main(String[] args) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://111.229.94.137:6379").setPassword("peng123");
        //构造Redisson
        RedissonClient redisson = Redisson.create(config);

        RBloomFilter<String> bloomFilter = redisson.getBloomFilter("nameList");
        //初始化布隆过滤器：预计元素为100000000L,误差率为3%,根据这两个参数会计算出底层的bit数组大小
        bloomFilter.tryInit(100000L,0.03);
        //将zhuge插入到布隆过滤器中
        bloomFilter.add("peng");
        bloomFilter.add("ju");

        //判断下面号码是否在布隆过滤器中
        System.out.println(bloomFilter.contains("spengju"));//false
        System.out.println(bloomFilter.contains("spj"));//false
        System.out.println(bloomFilter.contains("peng"));//true
        System.out.println(bloomFilter.contains("ju"));//true
    }
}

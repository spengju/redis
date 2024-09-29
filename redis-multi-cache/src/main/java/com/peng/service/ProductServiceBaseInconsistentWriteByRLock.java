package com.peng.service;

import com.alibaba.fastjson.JSON;
import com.peng.common.RedisKeyPrefixConst;
import com.peng.common.util.RedisUtil;
import com.peng.dao.ProductDao;
import com.peng.model.Product;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @Author: spengju
 * @Slogan: Day day no bug.
 * @Date: 2024/9/30 01:22
 * 解决:
 * 1.冷热分离
 * 2.缓存击穿
 * 3.缓存穿透
 * 4.突发性热点缓存重建:
 * redisson分布式锁实现双重检测,只针对特定的商品加锁
 * 5.添加修改锁，解决缓存和数据库双写不一致
 * 读写锁，缩小锁的粒度
 */
@Service
public class ProductServiceBaseInconsistentWriteByRLock implements ProductService {
    public static final Integer PRODUCT_CACHE_TIMEOUT = 60 * 60 * 24;
    public static final String LOCK_PRODUCT_HOT_CACHE_PREFIX = "lock:product:hot_cache:";

    public static final String LOCK_PRODUCT_UPDATE_PREFIX = "lock:product:update:";
    public static final String EMPTY_CACHE = "{}";

    @Autowired
    private ProductDao productDao;

    @Autowired
    private Redisson redisson;

    @Override
    public Product create(Product product) {
        Product productResult = null;
        RReadWriteLock readWriteLock = redisson.getReadWriteLock(LOCK_PRODUCT_UPDATE_PREFIX + product.getId());
        RLock writeLock = readWriteLock.writeLock();
        writeLock.lock();
        try {
            productResult = productDao.create(product);
            if (product != null) {
                RedisUtil.set(RedisKeyPrefixConst.PRODUCT_CACHE + productResult.getId(), JSON.toJSONString(productResult), genProductCacheTimeout(), TimeUnit.SECONDS);
            }
        } finally {
            writeLock.unlock();
        }
        return productResult;
    }

    @Override
    @Transactional
    public Product update(Product product) {
        Product productResult = null;
        RReadWriteLock readWriteLock = redisson.getReadWriteLock(LOCK_PRODUCT_UPDATE_PREFIX + product.getId());
        RLock writeLock = readWriteLock.writeLock();
        writeLock.lock();
        try {
            productResult = productDao.update(product);
            if (product != null) {
                RedisUtil.set(RedisKeyPrefixConst.PRODUCT_CACHE + productResult.getId(), JSON.toJSONString(productResult), genProductCacheTimeout(), TimeUnit.SECONDS);
            }
        } finally {
            writeLock.unlock();
        }
        return productResult;
    }

    @Override
    public Product get(Long productId) {
        Product product = null;
        String productCacheKey = RedisKeyPrefixConst.PRODUCT_CACHE + productId;

        product = getProductFromCache(productCacheKey);
        if (product != null) {
            return product;
        }

        // DCL双重检测
        RLock hotCacheLock = redisson.getLock(LOCK_PRODUCT_HOT_CACHE_PREFIX + productId);
        hotCacheLock.lock();
        try {
            product = getProductFromCache(productCacheKey);
            if (product != null) {
                return product;
            }
            RReadWriteLock readWriteLock = redisson.getReadWriteLock(LOCK_PRODUCT_UPDATE_PREFIX + productId);
            RLock rLock = readWriteLock.readLock();
            rLock.lock();
            try {
                product = productDao.get(productId);
                if (product != null) {
                    RedisUtil.set(productCacheKey, product, genProductCacheTimeout(), TimeUnit.SECONDS);
                } else {
                    /**
                     * 添加空缓存，解决缓存击穿和黑客通过不存在id攻击
                     */
                    RedisUtil.set(productCacheKey, EMPTY_CACHE, genEmptyCacheTimeout(), TimeUnit.SECONDS);
                }
            } finally {
                rLock.unlock();
            }

        } finally {
            hotCacheLock.unlock();
        }


        return product;
    }

    private Integer genProductCacheTimeout() {
        /**
         * 解决缓存击穿
         *  添加随机过期时间，防止大批缓存同时失效
         */
        return PRODUCT_CACHE_TIMEOUT + new Random().nextInt(5) * 60 * 60;
    }

    private Integer genEmptyCacheTimeout() {
        /**
         * 空缓存过期时间设置小一点，避免不必要的内存占用
         */
        return 60 + new Random().nextInt(30);
    }

    private Product getProductFromCache(String productCacheKey) {
        Product product = null;
        String productStr = RedisUtil.get(productCacheKey);
        if (!StringUtils.isEmpty(productStr)) {
            if (EMPTY_CACHE.equals(productStr)) {
                RedisUtil.set(productCacheKey, EMPTY_CACHE, genEmptyCacheTimeout(), TimeUnit.SECONDS);
                return new Product();
            }
            product = JSON.parseObject(productStr, Product.class);
            /**
             * 读延期:
             *   冷热分离:热门数据一直在缓存里面，冷门数据不会
             */
            RedisUtil.expire(productCacheKey, PRODUCT_CACHE_TIMEOUT, TimeUnit.SECONDS);
        }
        return product;
    }

}


















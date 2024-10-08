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

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author: spengju
 * @Slogan: Day day no bug.
 * @Date: 2024/9/30 01:05
 */
@Service
public class ProductServiceImpl implements ProductService{

    @Autowired
    private ProductDao productDao;

    @Autowired
    private Redisson redisson;

    public static final Integer PRODUCT_CACHE_TIMEOUT = 60 * 60 * 24;
    public static final String EMPTY_CACHE = "{}";
    public static final String LOCK_PRODUCT_HOT_CACHE_PREFIX = "lock:product:hot_cache:";
    public static final String LOCK_PRODUCT_UPDATE_PREFIX = "lock:product:update:";
    public static Map<String, Product> productMap = new ConcurrentHashMap<>();

    @Transactional
    public Product create(Product product) {
        Product productResult = productDao.create(product);
        RedisUtil.set(RedisKeyPrefixConst.PRODUCT_CACHE + productResult.getId(), JSON.toJSONString(productResult),
                genProductCacheTimeout(), TimeUnit.SECONDS);
        return productResult;
    }

    @Transactional
    public Product update(Product product) {
        Product productResult = null;
        //RLock updateProductLock = redisson.getLock(LOCK_PRODUCT_UPDATE_PREFIX + product.getId());
        RReadWriteLock readWriteLock = redisson.getReadWriteLock(LOCK_PRODUCT_UPDATE_PREFIX + product.getId());
        RLock writeLock = readWriteLock.writeLock();
        writeLock.lock();
        try {
            productResult = productDao.update(product);
            RedisUtil.set(RedisKeyPrefixConst.PRODUCT_CACHE + productResult.getId(), JSON.toJSONString(productResult),
                    genProductCacheTimeout(), TimeUnit.SECONDS);
            productMap.put(RedisKeyPrefixConst.PRODUCT_CACHE + productResult.getId(), product);
        } finally {
            writeLock.unlock();
        }
        return productResult;
    }

    public Product get(Long productId) {
        Product product = null;
        String productCacheKey = RedisKeyPrefixConst.PRODUCT_CACHE + productId;

        product = getProductFromCache(productCacheKey);
        if (product != null) {
            return product;
        }
        //DCL
        RLock hotCacheLock = redisson.getLock(LOCK_PRODUCT_HOT_CACHE_PREFIX + productId);
        hotCacheLock.lock();
        //boolean result = hotCacheLock.tryLock(3, TimeUnit.SECONDS);
        try {
            product = getProductFromCache(productCacheKey);
            if (product != null) {
                return product;
            }

            //RLock updateProductLock = redisson.getLock(LOCK_PRODUCT_UPDATE_PREFIX + productId);
            RReadWriteLock readWriteLock = redisson.getReadWriteLock(LOCK_PRODUCT_UPDATE_PREFIX + productId);
            RLock rLock = readWriteLock.readLock();
            rLock.lock();
            try {
                product = productDao.get(productId);
                if (product != null) {
                    RedisUtil.set(productCacheKey, JSON.toJSONString(product),
                            genProductCacheTimeout(), TimeUnit.SECONDS);
                    productMap.put(productCacheKey, product);
                } else {
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
        return PRODUCT_CACHE_TIMEOUT + new Random().nextInt(5) * 60 * 60;
    }

    private Integer genEmptyCacheTimeout() {
        return 60 + new Random().nextInt(30);
    }

    private Product getProductFromCache(String productCacheKey) {
        Product product = productMap.get(productCacheKey);
        if (product != null) {
            return product;
        }

        String productStr = RedisUtil.get(productCacheKey);
        if (!StringUtils.isEmpty(productStr)) {
            if (EMPTY_CACHE.equals(productStr)) {
                RedisUtil.expire(productCacheKey, genEmptyCacheTimeout(), TimeUnit.SECONDS);
                return new Product();
            }
            product = JSON.parseObject(productStr, Product.class);
            RedisUtil.expire(productCacheKey, genProductCacheTimeout(), TimeUnit.SECONDS); //读延期
        }
        return product;
    }
}



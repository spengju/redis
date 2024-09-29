package com.peng.service;

import com.alibaba.fastjson.JSON;
import com.peng.common.RedisKeyPrefixConst;
import com.peng.common.util.RedisUtil;
import com.peng.dao.ProductDao;
import com.peng.model.Product;
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
 *  冷热分离
 *  缓存击穿
 *  缓存穿透
 */
@Service
public class ProductServiceBaseExpire implements ProductService {
    public static final Integer PRODUCT_CACHE_TIMEOUT = 60 * 60 * 24;

    public static final String EMPTY_CACHE = "{}";

    @Autowired
    private ProductDao productDao;

    @Override
    public Product create(Product product) {
        Product productResult = productDao.create(product);
        RedisUtil.set(RedisKeyPrefixConst.PRODUCT_CACHE + productResult.getId(), JSON.toJSONString(productResult));
        return productResult;
    }

    @Override
    @Transactional
    public Product update(Product product) {
        Product productResult = productDao.update(product);
        RedisUtil.set(RedisKeyPrefixConst.PRODUCT_CACHE + productResult.getId(), JSON.toJSONString(productResult), genProductCacheTimeout(), TimeUnit.SECONDS);
        return null;
    }

    @Override
    public Product get(Long productId) {
        Product product = null;
        String productCacheKey = RedisKeyPrefixConst.PRODUCT_CACHE + productId;

        String productStr = RedisUtil.get(productCacheKey);
        if (!StringUtils.isEmpty(productStr)) {
            if (EMPTY_CACHE.equals(productStr)) {
                RedisUtil.set(productCacheKey, EMPTY_CACHE, genEmptyCacheTimeout(), TimeUnit.SECONDS);
                return null;
            }
            product = JSON.parseObject(productStr, Product.class);
            /**
             * 读延期:
             *   冷热分离:热门数据一直在缓存里面，冷门数据不会
             */
            RedisUtil.expire(productCacheKey, PRODUCT_CACHE_TIMEOUT, TimeUnit.SECONDS);
            return product;
        }

        product = productDao.get(productId);
        if (product != null) {
            RedisUtil.set(productCacheKey, product, genProductCacheTimeout(), TimeUnit.SECONDS);
        } else {
            /**
             * 添加空缓存，解决缓存击穿和黑客通过不存在id攻击
             */
            RedisUtil.set(productCacheKey, EMPTY_CACHE, genEmptyCacheTimeout(), TimeUnit.SECONDS);
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

}


















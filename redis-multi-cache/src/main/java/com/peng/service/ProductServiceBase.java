package com.peng.service;

import com.alibaba.fastjson.JSON;
import com.peng.common.RedisKeyPrefixConst;
import com.peng.common.util.RedisUtil;
import com.peng.dao.ProductDao;
import com.peng.model.Product;
import org.redisson.Redisson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: spengju
 * @Slogan: Day day no bug.
 * @Date: 2024/9/30 01:22
 */
@Service
public class ProductServiceBase implements ProductService{
    @Autowired
    private ProductDao productDao;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private Redisson redisson;

    @Override
    public Product create(Product product) {
        Product productResult = productDao.create(product);
        redisUtil.set(RedisKeyPrefixConst.PRODUCT_CACHE + productResult.getId(), JSON.toJSONString(productResult));
        return productResult;
    }

    @Override
    @Transactional
    public Product update(Product product) {
        Product productResult = productDao.update(product);
        redisUtil.set(RedisKeyPrefixConst.PRODUCT_CACHE + productResult.getId(), JSON.toJSONString(productResult));
        return null;
    }

    @Override
    public Product get(Long productId) {
        return null;
    }
}


















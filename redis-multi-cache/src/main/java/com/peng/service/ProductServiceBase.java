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

/**
 * @Author: spengju
 * @Slogan: Day day no bug.
 * @Date: 2024/9/30 01:22
 */
@Service
public class ProductServiceBase implements ProductService {

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
        RedisUtil.set(RedisKeyPrefixConst.PRODUCT_CACHE + productResult.getId(), JSON.toJSONString(productResult));
        return null;
    }

    @Override
    public Product get(Long productId) {
        Product product = null;
        String productCacheKey = RedisKeyPrefixConst.PRODUCT_CACHE + productId;

        String productStr = RedisUtil.get(productCacheKey);
        if (!StringUtils.isEmpty(productStr)) {
            product = JSON.parseObject(productStr, Product.class);
            return product;
        }
        product = productDao.get(productId);
        if (product != null) {
            RedisUtil.set(productCacheKey, JSON.toJSONString(product));

        }
        return product;
    }
}


















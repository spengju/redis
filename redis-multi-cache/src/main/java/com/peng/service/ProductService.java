package com.peng.service;

import com.peng.model.Product;

/**
 * @Author: spengju
 * @Slogan: Day day no bug.
 * @Date: 2024/9/30 01:11
 */
public interface ProductService {
    Product create(Product product);

    Product update(Product product);

    Product get(Long productId);
}

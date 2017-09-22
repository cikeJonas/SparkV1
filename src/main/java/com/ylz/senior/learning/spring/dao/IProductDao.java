package com.ylz.senior.learning.spring.dao;

import com.ylz.senior.learning.spring.beans.ProductBean;

import java.util.List;

/**
 * Created by Jonas on 2017/8/30.
 */
public interface IProductDao {
    List<ProductBean> getProducts();

    ProductBean getProductById(long id);
}

package com.ylz.seniorlearning.spring;

import com.ylz.seniorlearning.spring.beans.ProductBean;
import com.ylz.seniorlearning.spring.dao.IProductDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class HelloWord {
    @Autowired
    private IProductDao productDao;
    @Autowired
    private RedisCacheManager redisCacheManager;
    @ResponseBody
    @RequestMapping(value = "/test", method = RequestMethod.POST)
    public List<ProductBean> test() {
        return productDao.getProducts();
    }
}

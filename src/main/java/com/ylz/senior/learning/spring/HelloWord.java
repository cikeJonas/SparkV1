package com.ylz.senior.learning.spring;

import com.ylz.senior.learning.spring.beans.ProductBean;
import com.ylz.senior.learning.spring.dao.IProductDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class HelloWord {
    @Autowired
    private IProductDao productDao;
    @Autowired
    private RedisCacheManager redisCacheManager;

    @RequestMapping(value = "/test", method = RequestMethod.POST)
    @ResponseBody
    public List<ProductBean> test() {
        return productDao.getProducts();
    }

    @RequestMapping("springdemo")
    @ResponseBody
    public String springdemo(@RequestParam String name) {
        final long currentmilles = System.currentTimeMillis();
        final String preFix = "this is a simple test";
        return preFix + name;
    }
}

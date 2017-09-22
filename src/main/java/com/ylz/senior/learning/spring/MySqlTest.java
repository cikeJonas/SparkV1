package com.ylz.senior.learning.spring;

import com.ylz.senior.learning.spring.beans.ProductBean;
import com.ylz.senior.learning.spring.dao.IProductDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * Created by Jonas on 2017/9/21.
 */
@Controller
@RequestMapping("api")
public class MySqlTest {

    @Autowired
    private IProductDao productDao;
    @RequestMapping("spring")
    @ResponseBody
    public List<ProductBean> getName(@RequestParam("name") String name) {
        return productDao.getProducts();
    }
}

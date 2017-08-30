package com.ylz.seniorlearning.soa.imps;

import com.ylz.seniorlearning.soa.interfaces.ISayHelloService;

/**
 * Created by Jonas on 2017/8/23.
 */
public class SayHelloServiceImp implements ISayHelloService {
    public String sayHello(String hi) {
        if ("hello".equals(hi)) {
            return "hello";
        }
        return "byebye";
    }
}

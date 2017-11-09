package com.ylz.senior.learning.json;

import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * Created by Jonas on 2017/11/8.
 */
public class JsonTest {
    @Test
    public void test() {
        final String json = "{name: test, password: 123abc}";
        Gson g = new Gson();
        Person p = g.fromJson(json, Person.class);
        Assert.assertEquals("test", p.getName());

        Person pp = new Person("name", "password");
        String toJson = g.toJson(pp);
        Assert.assertEquals("{\"name\":\"name\",\"password\":\"password\"}", toJson);
        Person ppp = g.fromJson(toJson, Person.class);
        Assert.assertEquals("name", ppp.getName());

        Map<String, String> map = g.fromJson(json, Map.class);
        Assert.assertEquals("test", map.get("name"));
    }
}

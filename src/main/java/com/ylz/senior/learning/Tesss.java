package com.ylz.senior.learning;

/**
 * Created by Jonas on 2017/8/24.
 */
public class Tesss {
    private final String nnnnnn = "FDSA";

    public static void main(String[] args) {
        Upcating u = new Upcating();
        AcceptClassTest acceptClassTest = new AcceptClassTest();
        acceptClassTest.isAcceptClass(u);
        u.getName();
    }

}

class Upcating extends Parent {
    private String name;

    public Upcating() {
        super();
    }

    public Upcating(String name) {
        this.name = name;
    }

    public String getName() {
        for (int i = 0; i < 10; i++) {

        }
        return name;

    }

    public void setName(String name) {
        this.name = name;
    }

}

class Parent {
    public int add(int a, int b) {
        return a + b;
    }
}

class AcceptClassTest {
    public boolean isAcceptClass(Parent p) {
        return true;
    }
}
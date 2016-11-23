package cn.v5.rpc.test;


import cn.v5.rpc.annotation.RpcService;
import cn.v5.rpc.demo.User;
import org.springframework.stereotype.Component;

@Component
@RpcService(topic = "rpc/anno/service")
public class RpcServiceAnnoImpl2 {

    public String hello(String s) {
        return "hello " + s;
    }

    public boolean addUser(User user) {
        System.out.println("add user : " + user);
        return true;
    }

    public User getUser(String name) {
        return new User(name, "email", 10, 1, 1.1);
    }

    public int add(int v1, int v2) {
        return v1 + v2;
    }

    public int add2(int v1, int v2) {
        return v1 + v2;
    }

    public void delay(int delay, long time) {
        long now = System.currentTimeMillis();
        System.out.println("delay:dx  " + delay * 1000 + ":" + (now - time));
    }
}

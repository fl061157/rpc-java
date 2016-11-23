package cn.v5.rpc.test;

import cn.v5.rpc.annotation.RpcMethod;
import cn.v5.rpc.demo.User;
import org.springframework.stereotype.Component;

@Component
public class RpcServiceAnnoImpl {

    @RpcMethod(topic = "rpc/anno1")
    public User getUser(String name) {
        return new User(name, "email", 10, 1, 1.1);
    }

    @RpcMethod(topic = "rpc/anno2")
    public int add(int v1, int v2) {
        return v1 + v2;
    }

    @RpcMethod(topic = "rpc/anno3", alias = "addTwo")
    public int add2(int v1, int v2) {
        return v1 + v2;
    }
}

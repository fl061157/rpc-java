package cn.v5.rpc.test;

import cn.v5.rpc.RpcFuture;
import org.springframework.cache.annotation.Cacheable;

public interface HelloServiceAsync {
    RpcFuture<Void> callVoid();
    RpcFuture<Boolean> addUser(cn.v5.rpc.test.User arg1);
    RpcFuture<Integer> overloading2(String arg1, int arg2);
    RpcFuture<cn.v5.rpc.test.User> getUser(String arg1);
    RpcFuture<Void> callVoid2(String arg1);
    RpcFuture<Integer> add(int arg1, int arg2);
    RpcFuture<Integer> overloading(String arg1);
    RpcFuture<Void> exception();

    @Cacheable(value = "hello", unless = "#result eq null")
    RpcFuture<String> hello(String arg1);
}

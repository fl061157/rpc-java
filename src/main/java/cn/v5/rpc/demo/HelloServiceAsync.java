package cn.v5.rpc.demo;

import cn.v5.rpc.RpcFuture;

public interface HelloServiceAsync {
    RpcFuture<Void> callVoid();
    RpcFuture<Boolean> addUser(User arg1);
    RpcFuture<Integer> overloading2(String arg1, int arg2);
    RpcFuture<User> getUser(String arg1);
    RpcFuture<Void> callVoid2(String arg1);
    RpcFuture<Integer> add(int arg1, int arg2);
    RpcFuture<Integer> overloading(String arg1);
    RpcFuture<Void> exception();
    RpcFuture<String> hello(String arg1);
}

package cn.v5.rpc.reflect;

import java.lang.reflect.Method;
import java.util.function.Supplier;

public interface ProxyMethodInterceptor {
    Object invoke(boolean async, Object proxy, Method method, Object[] args, Supplier<?> supplier) throws Throwable;
}

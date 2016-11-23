package cn.v5.rpc.support;

import cn.v5.rpc.RpcFuture;
import cn.v5.rpc.reflect.ProxyMethodInterceptor;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.interceptor.CacheAspectSupport;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class CacheInterceptor extends CacheAspectSupport implements ProxyMethodInterceptor {

    private Executor executor;

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.state(executor != null, "Property 'executor' is required");

        if (this.getCacheOperationSource() == null){
            this.setCacheOperationSources(new AnnotationCacheOperationSource());
        }
        if (this.getErrorHandler() == null){
            this.setErrorHandler(new SimpleCacheErrorHandler());
        }
        super.afterPropertiesSet();
    }

    @Override
    public Object invoke(boolean async, Object proxy, Method method, Object[] args, Supplier<?> supplier) throws Throwable {

        if (async) {
            return invokeAsync(proxy, method, args, supplier);
        }

        CacheOperationInvoker aopAllianceInvoker = () -> {
            try {
                return supplier.get();
            } catch (Throwable ex) {
                throw new CacheOperationInvoker.ThrowableWrapper(ex);
            }
        };

        try {
            return execute(aopAllianceInvoker, proxy, method, args);
        } catch (CacheOperationInvoker.ThrowableWrapper th) {
            throw th.getOriginal();
        }

    }

    public Object invokeAsync(Object proxy, Method method, Object[] args, Supplier<?> supplier) throws Throwable {

        Object[] rets = new Object[1];

        CacheOperationInvoker aopAllianceInvoker = () -> {
            try {
                Object obj = supplier.get();
                if (obj != null) {
                    rets[0] = obj;
                    obj = null;
                }
                return obj;
            } catch (Throwable ex) {
                throw new CacheOperationInvoker.ThrowableWrapper(ex);
            }
        };

        try {
            Object obj = execute(aopAllianceInvoker, proxy, method, args);
            if (rets[0] != null) {
                RpcFuture rpcFuture = (RpcFuture) rets[0];
                rpcFuture.then(ret -> {
                    execute(() -> ret, proxy, method, args);
                });
                return rpcFuture;
            } else if (!(obj instanceof Future)) {
                return new RpcFutureCacheProxy<>(obj, executor);
            }

            return obj;
        } catch (CacheOperationInvoker.ThrowableWrapper th) {
            throw th.getOriginal();
        }

    }

}

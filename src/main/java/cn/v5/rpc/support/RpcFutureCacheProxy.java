package cn.v5.rpc.support;

import cn.v5.rpc.ConcurrentRpcFuture;
import cn.v5.rpc.RpcFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class RpcFutureCacheProxy<V> extends RpcFuture<V> {

    private V result;
    private Executor executor;
    public RpcFutureCacheProxy(V result, Executor executor) {
        this.result = result;
        this.executor = executor;
    }

    @Override
    public RpcFuture<V> then(Consumer<V> consumer) {
        executor.execute(() -> consumer.accept(result));
        return this;
    }

    @Override
    public RpcFuture<V> then(Runnable runnable) {
        executor.execute(runnable);
        return this;
    }

    @Override
    public RpcFuture<V> onError(Consumer<Exception> consumer) {
        return this;
    }

    @Override
    public <T> ConcurrentRpcFuture<V, T> join(RpcFuture<T> rpcFuture2) {
        return new ConcurrentRpcFuture<>(this, rpcFuture2);
    }

    @Override
    public V get() throws InterruptedException {
        return result;
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException,
            TimeoutException {
        return result;
    }

    @Override
    public void join() throws InterruptedException {
    }

    @Override
    public void join(long timeout, TimeUnit unit) throws InterruptedException,
            TimeoutException {
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        // FIXME
        return false;
    }

    @Override
    public boolean isCancelled() {
        // FIXME
        return false;
    }

    @Override
    public V getResult() {
        return result;
    }
}

package cn.v5.rpc;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class ConcurrentRpcFuture<T, V> {

    private volatile T result1;
    private volatile V result2;
    private volatile boolean result1Done;
    private volatile boolean result2Done;
    private BiConsumer<T, V> biConsumer;
    private AtomicBoolean done = new AtomicBoolean(false);
    private volatile Exception error1;
    private volatile Exception error2;
    private FourConsumer<T, Exception, V, Exception> fourConsumer;
    private AtomicBoolean errorDone = new AtomicBoolean(false);

    public ConcurrentRpcFuture(final RpcFuture<T> rf1, final RpcFuture<V> rf2) {
        rf1.then(v1 -> {
            result1 = v1;
            result1Done = true;
            checkResult();
        });
        rf2.then(v2 -> {
            result2 = v2;
            result2Done = true;
            checkResult();
        });

        rf1.onError(err -> {
            error1 = err;
            checkError();
        });

        rf2.onError(err -> {
            error2 = err;
            checkError();
        });
    }

    protected void checkResult() {
        if (result2Done && result1Done && biConsumer != null && done.compareAndSet(false, true)) {
            biConsumer.accept(result1, result2);
        }
    }

    public ConcurrentRpcFuture<T, V> then(BiConsumer<T, V> consumer) {
        biConsumer = consumer;
        checkResult();
        return this;
    }

    protected void checkError() {
        if ((error1 != null || error2 != null) && (result2Done || error2 != null) && (result1Done || error1 != null) && fourConsumer != null && errorDone.compareAndSet(false, true)) {
            fourConsumer.accept(result1, error1, result2, error2);
        }
    }

    public ConcurrentRpcFuture<T, V> onError(FourConsumer<T, Exception, V, Exception> consumer) {
        fourConsumer = consumer;
        checkError();
        return this;
    }

    @FunctionalInterface
    public interface FourConsumer<T, TE, V, VE> {
        void accept(T t, TE te, V v, VE ve);
    }

}

package cn.v5.rpc;

import org.msgpack.type.Value;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RpcFutureAdapterSyncImpl extends AbstractRpcFutureAdapter {

    public RpcFutureAdapterSyncImpl(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void attachCallback(Runnable callback) {
        if (!done) {
            this.callbacks.add(callback);
        } else {
            executor.execute(callback);
        }
    }

    @Override
    public void join() throws InterruptedException {
    }

    @Override
    public void join(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
    }

    @Override
    public void setResult(Value result, Value error) {
        if (done) {
            return;
        }
        this.result = result;
        this.error = error;
        this.done = true;
        callbacks.forEach(cb -> executor.execute(cb));
    }
}

package cn.v5.rpc;

import org.msgpack.type.Value;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface RpcFutureAdapter {
    void attachCallback(Runnable callback);

    boolean hasAttachCallback();

    void join() throws InterruptedException;

    void join(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException;

    boolean isDone();

    Value getResult();

    Value getError();

    void setResult(Value result, Value error);

    String getTraceId();

    void setTraceId(String traceId);
}

package cn.v5.rpc;

import org.msgpack.type.Value;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

public abstract class AbstractRpcFutureAdapter implements RpcFutureAdapter {

    protected volatile boolean done = false;
    protected Value result;
    protected Value error;
    protected List<Runnable> callbacks = new ArrayList<>();
    protected String traceId;
    protected Executor executor;


    public boolean isDone() {
        return done;
    }

    public Value getResult() {
        return result;
    }

    public Value getError() {
        return error;
    }

    public boolean hasAttachCallback() {
        return !callbacks.isEmpty();
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}

package cn.v5.rpc.dispatcher;

import cn.v5.rpc.Request;

public interface Dispatcher {
    void dispatch(Request request) throws Exception;
    DispatcherDescription getDispatcherDescription();
}

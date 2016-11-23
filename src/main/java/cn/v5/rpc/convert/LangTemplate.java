package cn.v5.rpc.convert;

import cn.v5.rpc.dispatcher.DispatcherDescription;

public interface LangTemplate {
    String source(String topic, DispatcherDescription dispatcherDescription);
}

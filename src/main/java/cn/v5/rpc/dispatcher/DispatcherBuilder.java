package cn.v5.rpc.dispatcher;

import org.msgpack.MessagePack;

import java.lang.reflect.Method;
import java.util.Map;

public interface DispatcherBuilder {

    Dispatcher build(Object handler, MessagePack messagePack) ;
    Dispatcher build(Object handler, Class iface, MessagePack messagePack);
    Dispatcher build(Object handler, MessagePack messagePack, Map<String, String> namesMap) ;
    Dispatcher build(Object handler, MessagePack messagePack, Method[] methods, Map<String, String> namesMap) ;
}
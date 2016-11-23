package cn.v5.rpc.dispatcher;

import cn.v5.rpc.reflect.MethodSelector;
import cn.v5.rpc.reflect.Reflect;
import org.msgpack.MessagePack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultDispatcherBuilder implements DispatcherBuilder {

    public Dispatcher build(Object handler, MessagePack messagePack) {
        return new MethodDispatcher(
                new Reflect(messagePack), handler);
    }

    @Override
    public Dispatcher build(Object handler, Class iface, MessagePack messagePack) {
        return new MethodDispatcher(
                new Reflect(messagePack), handler, iface);
    }

    @Override
    public Dispatcher build(Object handler, MessagePack messagePack, Map<String, String> namesMap) {
        return new MethodDispatcher(
                new Reflect(messagePack), handler, namesMap);
    }

    @Override
    public Dispatcher build(Object handler, MessagePack messagePack, Method[] methods, Map<String, String> namesMap) {
        List<Method> list = new ArrayList<>();
        for (Method method : methods){
            if (MethodSelector.isRpcServerMethod(method)){
                list.add(method);
            }
        }
        return new MethodDispatcher(
                new Reflect(messagePack), handler, null, list.toArray(new Method[list.size()]), namesMap);
    }

}

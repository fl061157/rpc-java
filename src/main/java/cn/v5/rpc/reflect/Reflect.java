package cn.v5.rpc.reflect;

//
// MessagePack-RPC for Java
//
// Copyright (C) 2010 FURUHASHI Sadayuki
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//


import org.msgpack.MessagePack;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class Reflect {

    private Map<String, Proxy<?>> proxyCache = new HashMap<>();

    private Map<Method, Invoker> invokerCache = new HashMap<>();

    private InvokerBuilder invokerBuilder;
    private ProxyBuilder proxyBuilder;

    public Reflect(MessagePack messagePack) {
        //invokerBuilder = new ReflectionInvokerBuilder(messagePack);
        invokerBuilder = new JavassistInvokerBuilder(messagePack);
        proxyBuilder = new ReflectionProxyBuilder(messagePack);
    }

    public Reflect( InvokerBuilder invokerBuilder,ProxyBuilder proxyBuilder) {
        this.invokerBuilder = invokerBuilder;
        this.proxyBuilder = proxyBuilder;
    }

    public synchronized <T> Proxy<T> getProxy(String topic, Class<T> iface) {
        String key = getProxyClassKey(topic, iface);
        Proxy<?> proxy = proxyCache.get(key);
        if (proxy == null) {
            proxy = proxyBuilder.buildProxy(topic, iface);// ProxyBuilder.build(iface);
            proxyCache.put(key, proxy);
        }
        return (Proxy<T>) proxy;
    }

    public synchronized Invoker getInvoker(Method method) {
        Invoker invoker = invokerCache.get(method);
        if (invoker == null) {
            invoker = invokerBuilder.buildInvoker(method);
            invokerCache.put(method, invoker);
        }
        return invoker;
    }

    private <T> String getProxyClassKey(String topic, Class<T> iface){
        StringBuilder sb = new StringBuilder();
        sb.append(topic).append("@").append(iface.getName());
        return sb.toString();
    }
}

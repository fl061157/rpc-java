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

import cn.v5.rpc.NotifyRunContext;
import cn.v5.rpc.RpcClient;
import cn.v5.rpc.RpcFuture;
import org.msgpack.*;
import org.msgpack.template.*;
import org.msgpack.type.Value;
import org.msgpack.unpacker.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class ReflectionProxyBuilder extends ProxyBuilder {

    private static Logger logger = LoggerFactory.getLogger(ReflectionProxyBuilder.class);

    private static class ReflectionMethodEntry {
        private String topic;
        private String rpcName;
        private Template returnTypeTemplate;
        private boolean async;
        private InvokerBuilder.ArgumentEntry[] argumentEntries;
        private Type returnType;

        public ReflectionMethodEntry(String topic, MethodEntry e, Type returnType, Template returnTypeTemplate) {
            this.topic = topic;
            this.rpcName = e.getRpcName();
            this.returnTypeTemplate = returnTypeTemplate;
            this.async = e.isAsync();
            this.argumentEntries = e.getArgumentEntries();
            this.returnType = returnType;
        }

        public String getRpcName() {
            return rpcName;
        }

        public String getTopic() {
            return topic;
        }

        public Type getReturnType() {
            return returnType;
        }

        public Template getReturnTypeTemplate() {
            return returnTypeTemplate;
        }

        public boolean isAsync() {
            return async;
        }

        public Object[] sort(Object[] args) {
            Object[] params = new Object[argumentEntries.length];

            for (int i = 0; i < argumentEntries.length; i++) {
                InvokerBuilder.ArgumentEntry e = argumentEntries[i];
                if (!e.isAvailable()) {
                    continue;
                }
                if (params.length < e.getIndex()) {
                    // FIXME
                }
                if (e.isRequired() && args[i] == null) {
                    // TODO type error
                }
                params[i] = args[e.getIndex()];
            }

            return params;
        }
    }

    public class ReflectionHandler implements InvocationHandler {
        private RpcClient rpcClient;
        private Map<Method, ReflectionMethodEntry> entryMap;
        private Class<?> iface;
        private ProxyMethodInterceptor methodInterceptor;

        public ReflectionHandler(RpcClient s, ProxyMethodInterceptor methodInterceptor, Map<Method, ReflectionMethodEntry> entryMap, Class<?> iface) {
            this.rpcClient = s;
            this.entryMap = entryMap;
            this.iface = iface;
            this.methodInterceptor = methodInterceptor;
        }

        public Object invoke(Object proxy, Method method, Object[] args) {
            ReflectionMethodEntry e = entryMap.get(method);
            if (e == null) {
                //logger.trace("e is null. method={}, args=", method, args);
                if ("toString".equals(method.getName()) && String.class.equals(method.getReturnType())) {
                    return iface.getName() + "$" + this;
                }
                return null;
            }

            if (methodInterceptor != null) {
                try {
                    return methodInterceptor.invoke(e.isAsync(), proxy, method, args, () -> invokeInternal(e, proxy, method, args));
                } catch (Throwable throwable) {
                    logger.error(throwable.getMessage(), throwable);
                    return null;
                }
            }

            return invokeInternal(e, proxy, method, args);
        }

        public Object invokeInternal(ReflectionMethodEntry e, Object proxy, Method method, Object[] args) {
            Object[] params = e.sort(args);
            NotifyRunContext nrc = NotifyRunContext.has();
            if (nrc != null) {
                rpcClient.delayAndAliveNotifyApply(nrc.getDelay(), nrc.getAlive(), e.getTopic(), e.getRpcName(), params);
                Type type = e.getReturnType();
                if (Void.TYPE == type) {
                    return 0;
                }
                if (Integer.TYPE == type) {
                    return 0;
                }
                if (Short.TYPE == type) {
                    return 0;
                }
                if (Byte.TYPE == type) {
                    return 0;
                }
                if (Boolean.TYPE == type) {
                    return false;
                }
                if (Long.TYPE == type) {
                    return 0L;
                }
                if (Float.TYPE == type) {
                    return 0F;
                }
                if (Double.TYPE == type) {
                    return 0D;
                }
                return null;
            } else if (e.isAsync()) {
                return rpcClient.callAsyncApply(e.getReturnTypeTemplate(), e.getTopic(), e.getRpcName(), params);
            } else {
                Value obj = rpcClient.callApply(e.getTopic(), e.getRpcName(), params);
                if (obj.isNilValue()) {
                    return null;
                } else {
                    Template tmpl = e.getReturnTypeTemplate();
                    if (tmpl == null) {
                        return null;
                    }
                    try {
                        return tmpl.read(new Converter(messagePack, obj), null);
                    } catch (IOException e1) {
                        logger.error(e1.getMessage(), e1);
                        return null;
                    }
                }
            }
        }
    }

    public class ReflectionProxy<T> implements Proxy<T> {
        private Class<T> iface;
        private Map<Method, ReflectionMethodEntry> entryMap;

        public ReflectionProxy(Class<T> iface, Map<Method, ReflectionMethodEntry> entryMap) {
            this.iface = iface;
            this.entryMap = entryMap;
        }

        public T newProxyInstance(RpcClient s, ProxyMethodInterceptor methodInterceptor) {
            ReflectionHandler handler = new ReflectionHandler(s, methodInterceptor, entryMap, iface);
            return (T) java.lang.reflect.Proxy.newProxyInstance(
                    iface.getClassLoader(), new Class[]{iface}, handler);
        }
    }

    private MessagePack messagePack;

    public ReflectionProxyBuilder(MessagePack messagePack) {

        this.messagePack = messagePack;
    }

    @Override
    public <T> Proxy<T> buildProxy(String topic, Class<T> iface, MethodEntry[] entries) {
        for (MethodEntry e : entries) {
            Method method = e.getMethod();
            int mod = method.getModifiers();
            if (!Modifier.isPublic(mod)) {
                method.setAccessible(true);
            }
        }

        Map<Method, ReflectionMethodEntry> entryMap = new HashMap<>();
        for (int i = 0; i < entries.length; i++) {
            MethodEntry e = entries[i];
            Template tmpl = null;
            if (!e.isReturnTypeVoid()) {
                tmpl = messagePack.lookup(e.getGenericReturnType());
            }
            entryMap.put(e.getMethod(), new ReflectionMethodEntry(topic, e, e.getGenericReturnType(), tmpl));
        }

        return new ReflectionProxy<T>(iface, entryMap);
    }
}


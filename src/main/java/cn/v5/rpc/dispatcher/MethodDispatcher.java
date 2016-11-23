package cn.v5.rpc.dispatcher;

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

import cn.v5.rpc.Request;
import cn.v5.rpc.annotation.RpcMethod;
import cn.v5.rpc.reflect.Invoker;
import cn.v5.rpc.reflect.MethodSelector;
import cn.v5.rpc.reflect.Reflect;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


public class MethodDispatcher implements Dispatcher {

    private static Logger logger = LoggerFactory.getLogger(MethodDispatcher.class);

    protected Map<String, Invoker> methodMap;
    protected Object target;
    protected Reflect reflect;

    protected DispatcherDescription dispatcherDescription;

    public MethodDispatcher(Reflect reflect, Object target) {
        this(reflect, target, target.getClass(), null);
    }

    public MethodDispatcher(Reflect reflect, Object target, Map<String, String> namesMap) {
        this(reflect, target, target.getClass(), namesMap);
    }

    public MethodDispatcher(Reflect reflect, Object target, Class<?> iface) {
        this(reflect, target, iface, MethodSelector.selectRpcServerMethod(iface), null);
    }

    // FIXME List<DispatchOption>
    public MethodDispatcher(Reflect reflect, Object target, Class<?> iface, Map<String, String> namesMap) {
        // FIXME check target instanceof iface
        this(reflect, target, iface, MethodSelector.selectRpcServerMethod(iface), namesMap);
    }

    public MethodDispatcher(Reflect reflect, Object target, Class<?> iface, Method[] methods, Map<String, String> namesMap) {
        // FIXME check target instanceof method.getClass()
        this.target = target;
        this.methodMap = new HashMap<>();
        this.reflect = reflect;
        String name = iface == null ? null:iface.getName();
        this.dispatcherDescription = new DispatcherDescription(name);
        if (namesMap == null || namesMap.size() < 1) {
            for (Method method : methods) {
                addMethodToMap(method.getName(), method);
            }
        } else {
            for (Method method : methods) {
                String aliasName = namesMap.get(method.getName());
                if (aliasName != null) {
                    addMethodToMap(aliasName, method);
                }
            }
        }
    }

    private void addMethodToMap(String name, Method method){
        RpcMethod rm = AnnotationUtils.findAnnotation(method, RpcMethod.class);
        String aliasName = name;
        if (rm != null){
            String newName = StringUtils.trimToNull(rm.alias());
            if (newName != null){
                aliasName = newName;
            }
        }
        if (methodMap.get(aliasName) == null) {
            methodMap.put(aliasName, reflect.getInvoker(method));
            this.dispatcherDescription.addMethod(aliasName, method);
        }else{
            logger.error("duplication method name : {}", aliasName);
        }
    }

    public void dispatch(Request request) throws Exception {
        Invoker ivk = methodMap.get(request.getMethodName());
        if (ivk == null) {
            logger.error("method:{} not found.", request.getMethodName());
            // FIXME
            throw new IOException(".CallError.NoMethodError");
        }
        ivk.invoke(target, request);
    }

    @Override
    public DispatcherDescription getDispatcherDescription() {
        return dispatcherDescription;
    }
}

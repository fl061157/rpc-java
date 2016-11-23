package cn.v5.rpc;

import cn.v5.mr.MRConnectionManager;
import cn.v5.rpc.annotation.RpcMethod;
import cn.v5.rpc.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.msgpack.MessagePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public class RpcServiceBeanAutoConfiguration implements BeanPostProcessor,
        ApplicationContextAware {
    private static Logger logger = LoggerFactory.getLogger(RpcServiceBeanAutoConfiguration.class);

    private MRConnectionManager mrConnectionManager;
    private MessagePack messagePack;
    private RpcServiceManager rpcServiceManager;
    private int perfetchSize;

    private ApplicationContext appCtx;


    public static final ReflectionUtils.MethodFilter RPC_SERVICE_METHOD_FILTER = method -> null != AnnotationUtils.findAnnotation(method, RpcMethod.class);

    public RpcServiceBeanAutoConfiguration() {

    }

    public void setPerfetchSize(int perfetchSize) {
        this.perfetchSize = perfetchSize;
    }

    public void setMrConnectionManager(MRConnectionManager mrConnectionManager) {
        this.mrConnectionManager = mrConnectionManager;
    }

    public void setMessagePack(MessagePack messagePack) {
        this.messagePack = messagePack;
    }

    public void setRpcServiceManager(RpcServiceManager rpcServiceManager) {
        this.rpcServiceManager = rpcServiceManager;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.appCtx = applicationContext;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        RpcService rpcServiceAnno = AnnotationUtils.findAnnotation(bean.getClass(), RpcService.class);
        if (rpcServiceAnno != null) {
            wireBean(bean, rpcServiceAnno);
        } else {
            Set<Method> methods = findHandlerMethods(bean.getClass(), RPC_SERVICE_METHOD_FILTER);
            if (methods != null && methods.size() > 0) {
                wireBean(bean, methods);
            }
        }
        return bean;
    }

    private void registerService(String topic, MRMessagePackInvokerService service) {
        if (mrConnectionManager != null) {
            mrConnectionManager.addListener(topic, perfetchSize, service);
        }
        if (rpcServiceManager != null) {
            rpcServiceManager.addService(topic, service);
        }
    }

    public void wireBean(final Object bean, RpcService rpcServiceAnno) {
        String topic = StringUtils.trimToNull(rpcServiceAnno.topic());
        if (topic == null) {
            logger.error("topic is null for bean '{}'", bean);
            return;
        }

        String face = StringUtils.trimToNull(rpcServiceAnno.face());
        Class<?> faceClass = null;
        if (face != null) {
            try {
                faceClass = ClassUtils.forName(face, null);
                if (faceClass != null && !faceClass.isInterface()) {
                    logger.error("class {} not is interface.", faceClass);
                    faceClass = null;
                }
            } catch (ClassNotFoundException e) {
                logger.error(e.getMessage());
            }
        }

        MRMessagePackInvokerService mrMessagePackInvokerService
                = faceClass == null ? new MRMessagePackInvokerService(topic, bean, messagePack, null) :
                new MRMessagePackInvokerService(topic, faceClass, bean, messagePack);
        logger.info("add rpc service, topic:bean '{}:{}'", topic, bean);
        registerService(topic, mrMessagePackInvokerService);
    }

    public void wireBean(final Object bean, final Set<Method> methods) {
        if (methods == null || methods.isEmpty()) {
            return;
        }

        RpcMethod rpcServiceAnno;

        for (final Method method : methods) {
            rpcServiceAnno = AnnotationUtils.findAnnotation(method, RpcMethod.class);
            if (rpcServiceAnno != null) {
                String aliasName = StringUtils.trimToNull(rpcServiceAnno.alias());
                String topic = StringUtils.trimToNull(rpcServiceAnno.topic());
                if (topic == null) {
                    continue;
                }
                if (aliasName == null) {
                    aliasName = method.getName();
                }

                Method[] ms = new Method[]{method};
                Map<String, String> namesMap = new HashMap<>();
                namesMap.put(method.getName(), aliasName);

                MRMessagePackInvokerService mrMessagePackInvokerService
                        = new MRMessagePackInvokerService(topic, bean, messagePack, ms, namesMap);

                logger.info("add rpc service topic:rpc name:'{}:{}' for bean:method name '{}:{}'",
                        topic, aliasName, bean, method.getName());
                registerService(topic, mrMessagePackInvokerService);
            }
        }
    }

    public static Set<Method> findHandlerMethods(Class<?> handlerType,
                                                 final ReflectionUtils.MethodFilter handlerMethodFilter) {
        final Set<Method> handlerMethods = new LinkedHashSet<>();

        if (handlerType == null) {
            return handlerMethods;
        }

        Set<Class<?>> handlerTypes = new LinkedHashSet<>();
        Class<?> specificHandlerType = null;
        if (!Proxy.isProxyClass(handlerType)) {
            handlerTypes.add(handlerType);
            specificHandlerType = handlerType;
        }
        handlerTypes.addAll(Arrays.asList(handlerType.getInterfaces()));
        for (Class<?> currentHandlerType : handlerTypes) {
            final Class<?> targetClass = (specificHandlerType != null ? specificHandlerType : currentHandlerType);
            ReflectionUtils.doWithMethods(currentHandlerType, method -> {
                Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
                Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
                if (handlerMethodFilter.matches(specificMethod) &&
                        (bridgedMethod == specificMethod || !handlerMethodFilter.matches(bridgedMethod))) {
                    handlerMethods.add(specificMethod);
                }
            }, ReflectionUtils.USER_DECLARED_METHODS);
        }
        return handlerMethods;
    }
}
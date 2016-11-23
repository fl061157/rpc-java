package cn.v5.rpc;

import cn.v5.mr.MRPublisher;
import cn.v5.rpc.reflect.ProxyMethodInterceptor;
import org.msgpack.MessagePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.Executor;

public class MRMessagePackInvokerProxyFactoryBean implements FactoryBean<Object>, InitializingBean, DisposableBean {

    private static Logger logger = LoggerFactory.getLogger(MRMessagePackInvokerProxyFactoryBean.class);
    
    private Object serviceProxy;

    private Class serviceInterface;
    private String serviceUrl;

    private RpcClient rpcClient;

    private MRPublisher publisher;
    private MessagePack messagePack;
    private Executor executor;
    private ProxyMethodInterceptor methodInterceptor;

    public void setPublisher(MRPublisher publisher) {
        this.publisher = publisher;
    }

    public void setServiceInterface(Class serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public void setMessagePack(MessagePack messagePack) {
        this.messagePack = messagePack;
    }

    public void setRpcClient(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public void setMethodInterceptor(ProxyMethodInterceptor methodInterceptor) {
        this.methodInterceptor = methodInterceptor;
    }

    @Override
    public void afterPropertiesSet() {
        if (this.serviceInterface == null) {
            throw new IllegalArgumentException("Property 'serviceInterface' is required");
        }
        if (this.serviceUrl == null) {
            throw new IllegalArgumentException("Property 'serviceUrl' is required");
        }
        if (this.publisher == null && this.rpcClient == null){
            throw new IllegalArgumentException("Property 'publisher' or 'rpcClient' is required");
        }
        if (rpcClient == null){
            if (messagePack == null){
                messagePack = new MessagePack();
            }
            this.rpcClient = new RpcClient(messagePack, publisher, executor);
        }

        if (executor == null){
            executor = rpcClient.getExecutor();
        }

        this.serviceProxy = this.rpcClient.proxy(serviceUrl, serviceInterface, methodInterceptor);
    }

    @Override
    public Object getObject() {
        return this.serviceProxy;
    }

    @Override
    public Class<?> getObjectType() {
        return serviceInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void destroy() throws Exception {
        this.rpcClient.close();
    }

}

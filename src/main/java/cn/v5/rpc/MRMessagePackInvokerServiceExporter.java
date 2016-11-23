package cn.v5.rpc;

import cn.v5.mr.MRConnectionManager;
import com.codahale.metrics.MetricRegistry;
import org.msgpack.MessagePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class MRMessagePackInvokerServiceExporter implements InitializingBean {

    private static Logger logger = LoggerFactory.getLogger(MRMessagePackInvokerServiceExporter.class);

    private String serviceUrl;
    private Class serviceInterface;
    private Object service;

    private MessagePack messagePack;

    private MRMessagePackInvokerService mrMessagePackInvokerService;

    private MRConnectionManager connectionManager;

    private RpcServiceManager rpcServiceManager;

    private int perfetchSize;

    private MetricRegistry registry;

    public void setServiceInterface(Class serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    public void setService(Object service) {
        this.service = service;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public void setMessagePack(MessagePack messagePack) {
        this.messagePack = messagePack;
    }

    public void setPerfetchSize(int perfetchSize) {
        this.perfetchSize = perfetchSize;
    }

    public void setConnectionManager(MRConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void setRpcServiceManager(RpcServiceManager rpcServiceManager) {
        this.rpcServiceManager = rpcServiceManager;
    }

    public void setRegistry(MetricRegistry registry) {
        this.registry = registry;
    }

    public MRMessagePackInvokerService getMrMessagePackInvokerService() {
        return mrMessagePackInvokerService;
    }

    @Override
    public void afterPropertiesSet() {
        if (this.serviceUrl == null) {
            throw new IllegalArgumentException("Property 'serviceUrl' is required");
        }
        if (this.service == null) {
            throw new IllegalArgumentException("Property 'service' is required");
        }
        if (this.messagePack == null) {
            this.messagePack = new MessagePack();
        }

        mrMessagePackInvokerService = new MRMessagePackInvokerService(serviceUrl, serviceInterface, service, messagePack);
        mrMessagePackInvokerService.setRegistry(registry);
        if (connectionManager != null) {
            connectionManager.addListener(this.serviceUrl, perfetchSize, mrMessagePackInvokerService);
        }
        if (rpcServiceManager != null){
            rpcServiceManager.addService(this.serviceUrl, mrMessagePackInvokerService);
        }
    }

}

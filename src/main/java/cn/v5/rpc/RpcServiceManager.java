package cn.v5.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.*;

public class RpcServiceManager implements ApplicationListener<ContextRefreshedEvent> {
    private static Logger logger = LoggerFactory.getLogger(RpcServiceManager.class);

    private Map<String, MRMessagePackInvokerService> serviceMap = Collections.synchronizedMap(new HashMap<>());

    public MRMessagePackInvokerService getService(String topic){
        return serviceMap.get(topic);
    }

    public void addService(String topic, MRMessagePackInvokerService service){
        logger.info("add service: {}:{}", topic ,service);
        serviceMap.put(topic, service);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        logger.debug("onApplicationEvent at {}", new Date(event.getTimestamp()).toGMTString());
    }

    public Set<String> getTopics(){
        return serviceMap.keySet();
    }

    public MRMessagePackInvokerService getMRMessagePackInvokerService(String topic){
        return serviceMap.get(topic);
    }
}

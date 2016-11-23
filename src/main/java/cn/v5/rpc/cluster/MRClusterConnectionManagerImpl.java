package cn.v5.rpc.cluster;

import cn.v5.mr.MRClient;
import cn.v5.mr.MRConnectionManager;
import cn.v5.mr.MRMessageListener;
import cn.v5.mr.MRPublisher;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Created by fangliang on 17/2/16.
 */
public class MRClusterConnectionManagerImpl implements MRConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(MRClusterConnectionManagerImpl.class);

    private MRConnectionManagerContainer mrConnectionManagerContainer;

    private String group;

    protected Map<String, MRMessageListener> messageListenerMap;

    private RoundRobinLoadStrategy roundRobinLoadStrategy = new RoundRobinLoadStrategy();

    private int perfetchSize;
    private int timeout;

    public void setPerfetchSize(int perfetchSize) {
        this.perfetchSize = perfetchSize;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public boolean init(String discoverURL, Executor executor) {
        try {
            return init(discoverURL, executor, null, null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    public boolean init(String discoverURL, String group, Executor executor, String provider, String clusterType) throws Exception {
        this.group = group;
        return init(discoverURL, executor, provider, clusterType);
    }

    public boolean init(String discoverURL, Executor executor, String provider, String clusterType) throws Exception {

        ClusterType cT;
        if (StringUtils.isBlank(clusterType) || (cT = ClusterType.getClusterType(clusterType)) == null) {
            logger.error("Parameters Must Be Correct ClusterType !");
            return false;
        }

        switch (cT) {
            case RpcServer:
            case RpcClient:
            case MrBizSub:
                this.mrConnectionManagerContainer = ManagerContainerFactory.getInstance().build(group, discoverURL, executor, provider, cT);
                this.mrConnectionManagerContainer.setPerfetchSize(perfetchSize);
                break;
            case MrBizPub:
                this.mrConnectionManagerContainer = ManagerContainerFactory.getInstance().buildBiz(group, discoverURL, executor, cT);
                this.mrConnectionManagerContainer.setPerfetchSize(perfetchSize);
                break;
            default:
                break;
        }

        if (this.mrConnectionManagerContainer == null) {
            logger.error("MrContainer Must Not Be Null !");
            return false;
        }

        try {
            this.mrConnectionManagerContainer.start(messageListenerMap);
            return true;
        } catch (Exception e) {
            throw e;
        }

    }

    @Override
    public void shutdown() {
        mrConnectionManagerContainer.shutdown();
    }

    @Override
    public void shutdownAndWait() throws InterruptedException {
        mrConnectionManagerContainer.shutdownAndWait();
    }

    @Override
    public void shutdownAndWait(int ms) throws InterruptedException {
        mrConnectionManagerContainer.shutdownAndWait();
    }

    @Override
    public void addListener(String topic, MRMessageListener mrMessageListener) {
        addListener(topic, perfetchSize, timeout, mrMessageListener);
    }

    @Override
    public void addListener(String topic, int perfetchSize, MRMessageListener mrMessageListener) {
        addListener(topic, perfetchSize, timeout, mrMessageListener);
    }

    @Override
    public void addListener(String topic, int perfetchSize, int timeout, MRMessageListener mrMessageListener) {
        if (mrConnectionManagerContainer != null) {
            Map<String, MRConnectionManager> managerCache = mrConnectionManagerContainer.getMrConnectionManagerCache();
            mrConnectionManagerContainer.addListener(topic, mrMessageListener);
            if (managerCache != null && managerCache.size() > 0) {
                managerCache.values().stream().forEach(manager -> {
                    if (!manager.containsListener(topic)) {
                        if (logger.isInfoEnabled()) {
                            logger.info("[AddListener] topic:{} , perfetchSize:{} ", topic, perfetchSize);
                        }
                        manager.addListener(topic,
                                perfetchSize == 0 ? 512 : perfetchSize,
                                timeout == 0 ? this.timeout : timeout, mrMessageListener);
                    }
                });
            }
        }
    }

    @Override
    public void addListeners(Map<String, MRMessageListener> stringMRMessageListenerMap) {
        if (mrConnectionManagerContainer != null && stringMRMessageListenerMap != null && stringMRMessageListenerMap.size() > 0) {

            Map<String, MRConnectionManager> managerCache = mrConnectionManagerContainer.getMrConnectionManagerCache();

            stringMRMessageListenerMap.entrySet().stream().forEach(e -> mrConnectionManagerContainer.addListener(e.getKey(), e.getValue()));

            if (managerCache != null && managerCache.size() > 0) {
                managerCache.values().stream().forEach(manager ->
                        stringMRMessageListenerMap.entrySet().stream().forEach(e -> {
                            if (!manager.containsListener(e.getKey())) {
                                if (logger.isInfoEnabled()) {
                                    logger.info("[AddListener] topic:{} , perfetchSize:{} ", e.getKey() , perfetchSize);
                                }
                                manager.addListener(e.getKey(), perfetchSize == 0 ? 512 : perfetchSize, e.getValue());
                            }
                        })
                );
            }
        }
    }

    @Override
    public MRPublisher getMRPublisher(int priority) {
        MRPublisher publisher = new MRClusterPublisher(mrConnectionManagerContainer, roundRobinLoadStrategy);
        publisher.setPriority(priority);
        return publisher;
    }

    @Override
    public MRClient getMRClient() { //ClusterClient
        return new MRClusterClient(mrConnectionManagerContainer, roundRobinLoadStrategy);
    }

    @Override
    public boolean containsListener(String s) {
        return messageListenerMap != null && messageListenerMap.containsKey(s);
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setMessageListenerMap(Map<String, MRMessageListener> messageListenerMap) {
        this.messageListenerMap = messageListenerMap;
    }
}

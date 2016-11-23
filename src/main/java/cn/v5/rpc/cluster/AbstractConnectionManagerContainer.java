package cn.v5.rpc.cluster;

import cn.v5.mr.MRConnectionManager;
import cn.v5.mr.MRMessageListener;
import cn.v5.mr.impl.MRConnectionManagerImpl;
import cn.v5.rpc.cluster.zookeeper.ZookeeperTransport;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by fangliang on 27/2/16.
 */
public abstract class AbstractConnectionManagerContainer implements MRConnectionManagerContainer {


    protected Map<String, MRMessageListener> mrMessageListenerMap = new HashMap<>();

    protected final String group;

    protected final String discoveryURL;

    protected DiscoverTransport discoverTransport;

    protected Executor businessExecutor;

    protected Map<String, MRConnectionManager> mrConnectionManagerCache = new ConcurrentHashMap<>();

    protected int perfetchSize;

    private AtomicBoolean isInitSuccess = new AtomicBoolean(false);

    private static final Logger logger = LoggerFactory.getLogger(AbstractConnectionManagerContainer.class);


    public AbstractConnectionManagerContainer(String group, String discoveryURL, Executor businessExecutor) {
        this.group = group;
        this.discoveryURL = discoveryURL;
        this.businessExecutor = businessExecutor;
        isInitSuccess.set(false);
    }

    @Override
    public void setPerfetchSize(int perfetchSize) {
        this.perfetchSize = perfetchSize;
    }

    @Override
    public void start() throws Exception {
        start(null);
    }

    @Override
    public void start(Map<String, MRMessageListener> messageListenerMap) throws Exception {
        if (!isInitSuccess.get()) {
            if (MapUtils.isNotEmpty(messageListenerMap)) {
                messageListenerMap.entrySet().stream().forEach(e -> {
                    if (logger.isInfoEnabled()) {
                        logger.info("MRConnectionManager Add Listener Key:{} ", e.getKey());
                    }
                    this.addListener(e.getKey(), e.getValue());
                });
            }

            if (discoverTransport == null) {
                discoverTransport = new ZookeeperTransport(discoveryURL, subscribePath(), this);
            }

            try {
                doStart();
                isInitSuccess.set(true);
            } catch (Exception e) {
                isInitSuccess.set(false);
                throw e;
            }
        }
    }

    protected abstract void doStart() throws Exception;


    protected abstract String subscribePath();


    @Override
    public void add(Map.Entry<String, String> entry) throws Exception {
        add(entry.getValue());
    }

    public void add(String url) throws Exception {

        if (!mrConnectionManagerCache.containsKey(url)) {
            MRConnectionManager mrConnectionManager = new MRConnectionManagerImpl();
            boolean init;
            try {
                init = mrConnectionManager.init(url, businessExecutor);
                if (init) {
                    mrConnectionManagerCache.put(url, mrConnectionManager);
                    if (mrMessageListenerMap != null && mrMessageListenerMap.size() > 0) {
                        mrMessageListenerMap.entrySet().stream().forEach(entry -> mrConnectionManager.addListener(entry.getKey(), perfetchSize, entry.getValue()));
                    }
                } else {
                    logger.error("[MRConnectionManager] init error url:{} ", url);
                    mrConnectionManager.shutdown();
                }
            } catch (Exception e) {
                logger.error("[MRConnectionManager] init error url:{} shutdown  ", url, e);
                mrConnectionManager.shutdown();
            }
        }
    }

    @Override
    public void addListener(String topic, MRMessageListener mrMessageListener) {
        if (logger.isDebugEnabled()) {
            logger.debug("AddListener Topic:{} ", topic);
        }
        mrMessageListenerMap.put(topic, mrMessageListener);
    }

    @Override
    public void remove(Map.Entry<String, String> entry) {
        remove(entry.getValue());
    }


    public void remove(String url) {
        if (logger.isDebugEnabled()) {
            logger.debug("Remove Url:{} ", url);
        }
        MRConnectionManager mrConnectionManager = mrConnectionManagerCache.get(url);
        if (mrConnectionManager != null) {
            mrConnectionManagerCache.remove(url);
            mrConnectionManager.shutdown();
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("MRConnectionManager Not Exits Url:{}", url);
            }
        }
    }

    @Override
    public void refresh() {
        Map<String, String> pD = discoverTransport.findPD();
        if (MapUtils.isEmpty(pD)) {
            logger.error("Group Path :{} Is Null !", group);
            return;
        }
        refresh(new HashSet<>(pD.values()));
    }


    protected void refresh(Set<String> urlSet) {
        if (CollectionUtils.isEmpty(urlSet)) {
            logger.error("Refresh Collection Is Empty !");
        }
        urlSet.stream().forEach(url -> {
            try {
                add(url);
            } catch (Exception e) {
                logger.error("", e);
            }
        });
        mrConnectionManagerCache.entrySet().stream().forEach(entry -> {
            if (!urlSet.contains(entry.getKey())) {
                remove(entry.getKey());
            }
        });
    }

    @Override
    public void shutdown() {
        Map<String, MRConnectionManager> managerCache = this.mrConnectionManagerCache;
        if (managerCache != null && managerCache.size() > 0) {
            managerCache.values().stream().forEach(mcm -> mcm.shutdown());
        }
        managerCache.clear();
    }

    @Override
    public void shutdownAndWait() throws InterruptedException {
        Map<String, MRConnectionManager> managerCache = this.mrConnectionManagerCache;
        int count = managerCache.size();
        final CountDownLatch countDownLatch;
        if (count > 0) {
            countDownLatch = new CountDownLatch(count);
            managerCache.values().parallelStream().forEach(mcm -> {
                try {
                    mcm.shutdownAndWait();
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                } finally {
                    countDownLatch.countDown();
                }
            });
            countDownLatch.await();
        }
    }

    @Override
    public Map<String, MRConnectionManager> getMrConnectionManagerCache() {
        return new HashMap<>(this.mrConnectionManagerCache);
    }

    public DiscoverTransport getDiscoverTransport() {
        return discoverTransport;
    }

    public Executor getBusinessExecutor() {
        return businessExecutor;
    }

    public String getGroup() {
        return group;
    }

    public String getDiscoveryURL() {
        return discoveryURL;
    }
}

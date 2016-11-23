package cn.v5.rpc.cluster;

import cn.v5.mr.MRMessageListener;
import cn.v5.mr.MRSubscriber;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Created by fangliang on 17/2/16.
 */
public class MRClusterConnectionManagerSpring extends MRClusterConnectionManagerImpl implements InitializingBean, DisposableBean {

    private String discoverURL;

    private Executor executor;

    private String provider;

    protected String clusterType;

    private Map<String, MessageListener> listenerMap;

    private static final Logger logger = LoggerFactory.getLogger(MRClusterConnectionManagerSpring.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        addListener();
        if (!init(discoverURL, executor, provider, clusterType)) {
            logger.error("mr cluster connection manager init fail.");
        }
    }

    @Override
    public void destroy() throws Exception {
        this.shutdownAndWait();
    }

    public void setDiscoverURL(String discoverURL) {
        this.discoverURL = discoverURL;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public void setClusterType(String clusterType) {
        this.clusterType = clusterType;
    }

    public void setListenerMap(Map<String, MessageListener> listenerMap) {
        this.listenerMap = listenerMap;
    }

    private void addListener() {
        if (MapUtils.isNotEmpty(listenerMap)) {
            if (messageListenerMap == null) {
                messageListenerMap = new HashMap<>();
            }

            listenerMap.entrySet().stream().forEach(e -> {
                if (logger.isInfoEnabled()) {
                    logger.info("Add Listener Topic:{} ", e.getKey());
                }
                messageListenerMap.put(e.getKey(), new MRInternalConsumer(e.getValue() ));
            });
            this.setListenerMap(listenerMap);
        }
    }

    /**
     * Simple Consumer
     */
    static class MRInternalConsumer implements MRMessageListener {
        private MessageListener messageListener;

        public MRInternalConsumer(MessageListener messageListener ) {
            this.messageListener = messageListener;

        }

        @Override
        public void onMessage(MRSubscriber subscriber, long messageID, byte[] message) {
            if (logger.isDebugEnabled()) {
                logger.debug("Receive Message.id : {}", messageID);
            }
            try {
                messageListener.onMessage(message);
            } catch (Throwable throwable) {
                logger.error("handle error:" + throwable.getMessage(), throwable);
            } finally {
                subscriber.ackOk(messageID);
            }

        }
    }


}

package cn.v5.rpc.cluster;

import cn.v5.mr.*;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by fangliang on 27/2/16.
 */
public class MRClusterClient implements MRClient {

    private MRConnectionManagerContainer managerContainer;

    private LoadStrategy<MRConnectionManager> loadStrategy;

    private static final Logger logger = LoggerFactory.getLogger(MRClusterClient.class);

    public MRClusterClient(MRConnectionManagerContainer managerContainer, LoadStrategy<MRConnectionManager> loadStrategy) {
        this.managerContainer = managerContainer;
        this.loadStrategy = loadStrategy;
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stopAndWait() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stopAndWait(int ms) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public int pub(String topic, byte[] bytes) {
        return this.pub(topic, bytes, 1);
    }


    @Override
    public int pub(String topic, byte[] bytes, int priority) {
        return iteratorMRClientForInt(client -> client.pub(topic, bytes, priority));
    }

    @Override
    public int pub(String topic, byte[] bytes, MessageAttribute messageAttribute) {
        return iteratorMRClientForInt(client -> client.pub(topic, bytes, messageAttribute));
    }

    @Override
    public int pub(String topic, byte[] bytes, MessageAttribute messageAttribute, MessageResultContext mrc) {
        return iteratorMRClientForInt(client -> client.pub(topic, bytes, messageAttribute, mrc));
    }

    @Override
    public int pubDelay(String topic, byte[] bytes, int delay_secs) {
        return this.pubDelay(topic, bytes, delay_secs, 1);
    }

    @Override
    public int pubDelay(String topic, byte[] bytes, int delay_secs, int priority) {
        return iteratorMRClientForInt(client -> client.pubDelay(topic, bytes, delay_secs, priority));
    }

    @Override
    public int pubSort(String topic, byte[] bytes, String sortKey) {
        return pubSort(topic, bytes, sortKey, 1);
    }

    @Override
    public int pubSort(String topic, byte[] bytes, String sortKey, int priority) {
        return iteratorMRClientForInt(client -> client.pubSort(topic, bytes, sortKey, priority));
    }

    @Override
    public int pubRepeat(String topic, byte[] bytes, int interval, int times) {
        return pubRepeat(topic, bytes, interval, times, 1);
    }

    @Override
    public int pubRepeat(String topic, byte[] bytes, int interval, int times, int priority) {
        return iteratorMRClientForInt(client -> client.pubRepeat(topic, bytes, interval, times, priority));
    }

    @Override
    public int pubDirect(String topic, byte[] bytes, MessageResultContext messageResultContext) {
        return pubDirect(topic, bytes, messageResultContext, 1);
    }

    @Override
    public int pubDirect(String topic, byte[] bytes, MessageResultContext mrc, int priority) {
        return iteratorMRClientForInt(client -> client.pubDirect(topic, bytes, mrc, priority));
    }

    enum PubAction {
        Pub, PubDelay, PubSort, PubRepeat, PubDirect, PubMessageAttribute;
    }

    static class MContext {
        String topic;
        byte[] data;
        int interval;
        int times;
        int delaySecs;
        String sortKey;
        MessageResultContext mc;
        int priority;
        MessageAttribute messageAttribute;

        public MContext(String topic, byte[] data) {
            this.topic = topic;
            this.data = data;
        }

        public MContext buildInterval(int interval) {
            this.interval = interval;
            return this;
        }

        public MContext buildTimes(int times) {
            this.times = times;
            return this;
        }

        public MContext buildDelaySecs(int delaySecs) {
            this.delaySecs = delaySecs;
            return this;
        }

        public MContext buildSortKey(String sortKey) {
            this.sortKey = sortKey;
            return this;
        }

        public MContext buildMessageAttribute(MessageAttribute messageAttribute) {
            this.messageAttribute = messageAttribute;
            return this;
        }

        public MContext buildMessageResultContext(MessageResultContext mc) {
            this.mc = mc;
            return this;
        }

        public MContext buildpPriority(int priority) {
            this.priority = priority;
            return this;
        }
    }

    static class Mc implements MessageCallback {
        private Iterator<MRClient> iterator;
        private PubAction pubAction;
        private MContext mContext;
        private MessageCallback callback;

        public Mc(Iterator<MRClient> iterator, MContext mContext, PubAction pubAction, MessageCallback callback) {
            this.iterator = iterator;
            this.mContext = mContext;
            this.pubAction = pubAction;
            this.callback = callback;
        }

        public void setIterator(Iterator<MRClient> iterator) {
            this.iterator = iterator;
        }

        @Override
        public void on(int status, long id, byte[] bytes) {
            if (status != 0) {
                if (iterator.hasNext()) {
                    switch (pubAction) {
                        case Pub:
                            iterator.next().pub(mContext.topic, mContext.data, Mc.this, mContext.priority);
                            break;
                        case PubDelay:
                            iterator.next().pubDelay(mContext.topic, mContext.data, mContext.delaySecs, Mc.this, mContext.priority);
                            break;
                        case PubDirect:
                            iterator.next().pubDirect(mContext.topic, mContext.data, Mc.this, mContext.priority);
                            break;
                        case PubRepeat:
                            iterator.next().pubRepeat(mContext.topic, mContext.data, mContext.interval, mContext.times, Mc.this, mContext.priority);
                            break;
                        case PubSort:
                            iterator.next().pubSort(mContext.topic, mContext.data, mContext.sortKey, Mc.this, mContext.priority);
                            break;
                        case PubMessageAttribute:
                            iterator.next().pub(mContext.topic, mContext.data, mContext.messageAttribute, Mc.this);
                            break;
                        default:
                            break;
                    }
                } else {
                    callback.on(status, id, bytes);
                }
            } else {
                callback.on(status, id, bytes);
            }
        }
    }

    @Override
    public boolean pub(String topic, byte[] bytes, MessageCallback messageCallback) {
        return pub(topic, bytes, messageCallback, 1);
    }

    @Override
    public boolean pub(String topic, byte[] bytes, MessageCallback pcb, int priority) {
        Mc mc = new Mc(null, new MContext(topic, bytes).buildpPriority(priority), PubAction.Pub, pcb);
        return iteratorMRClientForAsync((iterator, client) -> {
            mc.setIterator(iterator);
            return client.pub(topic, bytes, mc, priority);
        });
    }

    @Override
    public boolean pub(String topic, byte[] bytes, MessageAttribute messageAttribute, MessageCallback pcb) {
        Mc mc = new Mc(null, new MContext(topic, bytes).buildMessageAttribute(messageAttribute), PubAction.PubMessageAttribute, pcb);
        return iteratorMRClientForAsync((iterator, client) -> {
            mc.setIterator(iterator);
            return client.pub(topic, bytes, messageAttribute, mc);
        });
    }

    @Override
    public boolean pubDelay(String topic, byte[] bytes, int delay_secs, MessageCallback messageCallback) {
        return pubDelay(topic, bytes, delay_secs, messageCallback, 1);
    }

    @Override
    public boolean pubDelay(String topic, byte[] bytes, int delay_secs, MessageCallback pcb, int priority) {
        Mc mc = new Mc(null, new MContext(topic, bytes).buildDelaySecs(delay_secs).buildpPriority(priority), PubAction.PubDelay, pcb);
        return iteratorMRClientForAsync((iterator, client) -> {
            mc.setIterator(iterator);
            return client.pubDelay(topic, bytes, delay_secs, mc, priority);
        });
    }

    @Override
    public boolean pubSort(String topic, byte[] bytes, String sortKey, MessageCallback messageCallback) {
        return pubSort(topic, bytes, sortKey, messageCallback, 1);
    }

    @Override
    public boolean pubSort(String topic, byte[] bytes, String sortKey, MessageCallback pcb, int priority) {
        Mc mc = new Mc(null, new MContext(topic, bytes).buildSortKey(sortKey).buildpPriority(priority), PubAction.PubSort, pcb);
        return iteratorMRClientForAsync((iterator, client) -> {
            mc.setIterator(iterator);
            return client.pubSort(topic, bytes, sortKey, mc, priority);
        });
    }

    @Override
    public boolean pubRepeat(String topic, byte[] bytes, int interval, int times, MessageCallback pcb, int priority) {
        Mc mc = new Mc(null, new MContext(topic, bytes).buildInterval(interval).buildTimes(times).buildpPriority(priority), PubAction.PubRepeat, pcb);
        return iteratorMRClientForAsync((iterator, client) -> {
            mc.setIterator(iterator);
            return client.pubRepeat(topic, bytes, interval, times, mc, priority);
        });
    }

    @Override
    public boolean pubUnsafe(String topic, byte[] bytes, int priority) {
        return false;
    }

    @Override
    public boolean pubRepeat(String topic, byte[] bytes, int interval, int times, MessageCallback messageCallback) {
        return pubRepeat(topic, bytes, interval, times, messageCallback, 1);
    }

    @Override
    public boolean pubDirect(String topic, byte[] bytes, MessageCallback messageCallback) {
        return pubDirect(topic, bytes, messageCallback, 1);
    }

    @Override
    public boolean pubDirect(String topic, byte[] bytes, MessageCallback pcb, int priority) {
        Mc mc = new Mc(null, new MContext(topic, bytes).buildpPriority(priority), PubAction.PubDirect, pcb);
        return iteratorMRClientForAsync((iterator, client) -> {
            mc.setIterator(iterator);
            return client.pubDirect(topic, bytes, mc, priority);
        });
    }

    @Override
    public boolean pub(String topic) {
        return iteratorMRClientForBoolean(client -> client.pub(topic));
    }

    @Override
    public boolean sub(String topic, MessageCallback messageCallback) {
        throw new UnsupportedOperationException("MRClusterClient Not Support Sub !");
    }

    @Override
    public boolean sub(String topic, int i, MessageCallback messageCallback) {
        throw new UnsupportedOperationException("MRClusterClient Not Support Sub !");
    }

    @Override
    public boolean sub(String s, int i, int timeout, MessageCallback messageCallback) {
        throw new UnsupportedOperationException("MRClusterClient Not Support Sub !");
    }

    @Override
    public boolean sub(String s, int i, int timeout, int subType, MessageCallback messageCallback) {
        throw new UnsupportedOperationException("MRClusterClient Not Support Sub !");
    }

    @Override
    public boolean ack(String s, long l) { // MRClusterClient Support ?
        throw new UnsupportedOperationException("MRClusterClient Not Support Ack !");
    }

    @Override
    public boolean ack(String s, long l, int i) {
        throw new UnsupportedOperationException("MRClusterClient Not Support Ack !");
    }

    @Override
    public boolean ack(String s, long l, int i, byte[] bytes) {
        throw new UnsupportedOperationException("MRClusterClient Not Support Ack !");
    }

    @Override
    public boolean unsub(String s) {
        throw new UnsupportedOperationException("MRClusterClient Not Support UnSub !");
    }

    @Override
    public boolean cancel(String s, long l) {
        throw new UnsupportedOperationException("MRClusterClient Not Support Cancel !");
    }

    @Override
    public boolean pubUnsafe(String s, byte[] bytes) {
        return iteratorMRClientForBoolean(client -> client.pubUnsafe(s, bytes));
    }

    @Override
    public boolean subUnsafe(String s, MessageCallback messageCallback) {
        throw new UnsupportedOperationException("MRClusterClient Not Support subUnsafe !");
    }

    @Override
    public boolean subUnsafe(String s, int i, MessageCallback messageCallback) {
        throw new UnsupportedOperationException("MRClusterClient Not Support subUnsafe !");
    }

    protected List<MRConnectionManager> findMRConnectionManager() {
        Map<String, MRConnectionManager> managerMap = this.managerContainer.getMrConnectionManagerCache();
        List<MRConnectionManager> list = loadStrategy.find(new ArrayList<>(managerMap.values()));
        if (CollectionUtils.isEmpty(list)) {
            logger.error("Find MRConnectionManager List Empty !");
        }
        return list;
    }

    protected int iteratorMRClientForInt(Function<MRClient, Integer> function){
        List<MRConnectionManager> managerList = findMRConnectionManager();
        if (CollectionUtils.isEmpty(managerList)) {
            return -1;
        }
        Iterator<MRConnectionManager> iterator = managerList.iterator();
        int result;
        do {
            result = function.apply(iterator.next().getMRClient());
        } while (result != 0 && iterator.hasNext());
        return result;
    }

    protected boolean iteratorMRClientForBoolean(Function<MRClient, Boolean> function){
        List<MRConnectionManager> managerList = findMRConnectionManager();
        if (CollectionUtils.isEmpty(managerList)) {
            return false;
        }
        Iterator<MRConnectionManager> iterator = managerList.iterator();
        boolean result;
        do {
            result = function.apply(iterator.next().getMRClient());
        } while (!result && iterator.hasNext());
        return result;
    }

    protected boolean iteratorMRClientForAsync(BiFunction<Iterator<MRClient>, MRClient, Boolean> function){
        List<MRConnectionManager> managerList = findMRConnectionManager();
        if (CollectionUtils.isEmpty(managerList)) {
            return false;
        }
        List<MRClient> clientList = managerList.stream().map(m -> m.getMRClient()).collect(Collectors.toList());
        Iterator<MRClient> iterator = clientList.iterator();
        boolean result;
        do {
            MRClient client = iterator.next();
            result = function.apply(iterator, client);
        } while (!result && iterator.hasNext());
        return result;
    }

}

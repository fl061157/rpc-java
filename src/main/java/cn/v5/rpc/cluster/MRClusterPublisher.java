package cn.v5.rpc.cluster;

import cn.v5.mr.*;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by fangliang on 19/2/16.
 */
public class MRClusterPublisher implements MRPublisher {

    private static final Logger logger = LoggerFactory.getLogger(MRClusterPublisher.class);

    private MRConnectionManagerContainer managerContainer;
    private LoadStrategy<MRConnectionManager> loadStrategy;
    private int priority;

    public MRClusterPublisher(MRConnectionManagerContainer managerContainer, LoadStrategy<MRConnectionManager> loadStrategy) {
        this.managerContainer = managerContainer;
        this.loadStrategy = loadStrategy;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }

    protected MRConnectionManager getMRConnectionManager() {
        Map<String, MRConnectionManager> managerMap = this.managerContainer.getMrConnectionManagerCache();
        MRConnectionManager manager = loadStrategy.get(new ArrayList<>(managerMap.values()));
        if (manager == null) {
            logger.error("Get MRConnectionManager Empty !");
        }
        return manager;
    }

    protected List<MRConnectionManager> findMRConnectionManager() {
        Map<String, MRConnectionManager> managerMap = this.managerContainer.getMrConnectionManagerCache();
        List<MRConnectionManager> list = loadStrategy.find(new ArrayList<>(managerMap.values()));
        if (CollectionUtils.isEmpty(list)) {
            logger.error("Find MRConnectionManager List Empty !");
        }
        return list;
    }

    protected boolean iteratorMRPublisherForBoolean(Function<MRPublisher, Boolean> function) {
        List<MRConnectionManager> managerList = findMRConnectionManager();
        if (CollectionUtils.isEmpty(managerList)) {
            return false;
        }
        Iterator<MRConnectionManager> iterator = managerList.iterator();
        boolean result;
        do {
            result = function.apply(iterator.next().getMRPublisher(priority));
        } while (!result && iterator.hasNext());
        return result;
    }

    protected MessageResultContext iteratorMRPublisherForMessageResultContext(Function<MRPublisher, MessageResultContext> function) {
        List<MRConnectionManager> managerList = findMRConnectionManager();
        if (CollectionUtils.isEmpty(managerList)) {
            return null;
        }
        Iterator<MRConnectionManager> iterator = managerList.iterator();
        MessageResultContext result = null;
        do {
            result = function.apply(iterator.next().getMRPublisher(priority));
        } while (result == null && iterator.hasNext());
        return result;
    }

    protected boolean iteratorMRPublisherForAsync(BiFunction<Iterator<MRConnectionManager>, MRPublisher, Boolean> function) {
        List<MRConnectionManager> managerList = findMRConnectionManager();
        if (CollectionUtils.isEmpty(managerList)) {
            return false;
        }
        Iterator<MRConnectionManager> mrConnectionManagerIterator = managerList.iterator();
        List<MRPublisher> publisherList = managerList.stream().map(m -> m.getMRPublisher(priority)).collect(Collectors.toList());
        Iterator<MRPublisher> iterator = publisherList.iterator();
        boolean result;
        do {
            MRPublisher publisher = iterator.next();
            result = function.apply(mrConnectionManagerIterator, publisher);
        } while (!result && iterator.hasNext());
        return result;
    }

    @Override
    public boolean syncPub(String topic, byte[] data) {
        return iteratorMRPublisherForBoolean(publisher -> publisher.syncPub(topic, data));
    }

    @Override
    public boolean syncPub(String topic, byte[] data, MessageAttribute messageAttribute) {
        return iteratorMRPublisherForBoolean(publisher -> publisher.syncPub(topic, data, messageAttribute));
    }

    @Override
    public MessageResultContext syncPubForAck(String topic, byte[] data, MessageAttribute messageAttribute) {
        return iteratorMRPublisherForMessageResultContext(publisher -> publisher.syncPubForAck(topic, data, messageAttribute));
    }

    @Override
    public boolean syncPubDelay(String topic, byte[] data, int sec) {
        return iteratorMRPublisherForBoolean(publisher -> publisher.syncPubDelay(topic, data, sec));
    }

    @Override
    public boolean syncPubSort(String topic, byte[] data, String sortKey) {
        return iteratorMRPublisherForBoolean(publisher -> publisher.syncPubSort(topic, data, sortKey));
    }

    @Override
    public MessageResultContext syncPubDirect(String topic, byte[] data) { //TODO MessageResultContext Result 概念  result 0 or 1
        List<MRConnectionManager> managerList = findMRConnectionManager();
        if (CollectionUtils.isEmpty(managerList)) {
            return null;
        }
        Iterator<MRConnectionManager> iterator = managerList.iterator();
        MessageResultContext result = null;
        do {
            MRPublisher publisher = iterator.next().getMRPublisher(priority);
            if (publisher != null ){
                result = publisher.syncPubDirect(topic, data);
            }else{
                logger.warn("publisher is null.");
            }
        } while (result == null && iterator.hasNext());
        return result;
    }

    @Override
    public Future<MessageResultContext> asyncPub(String topic, byte[] data) {
        List<MRConnectionManager> managerList = findMRConnectionManager();
        if (CollectionUtils.isEmpty(managerList)) {
            return null;
        }
        Iterator<MRConnectionManager> iterator = managerList.iterator();
        Future<MessageResultContext> result = null;
        do {
            MRPublisher publisher = iterator.next().getMRPublisher(priority);
            if (publisher != null ){
                result = publisher.asyncPub(topic, data);
            }else{
                logger.warn("publisher is null.");
            }
        } while (result == null && iterator.hasNext());
        return result;
    }

    @Override
    public boolean asyncPub(String topic, byte[] data, MessageCallback callback) {
        GuaranteeMessageCallback cb = new GuaranteeMessageCallback(null, Way.AsyncPub,
                new MessageContext(topic, data).buildPriority(priority), callback);
        return iteratorMRPublisherForAsync((iterator, publisher) -> {
            cb.setIterator(iterator);
            return publisher.asyncPub(topic, data, cb);
        });
    }

    @Override
    public boolean asyncPub(String topic, byte[] data, MessageAttribute messageAttribute, MessageCallback callback) {
        GuaranteeMessageCallback cb = new GuaranteeMessageCallback(null, Way.AsyncPubMessageAttribute,
                new MessageContext(topic, data).buildMessageAttribute(messageAttribute), callback);
        return iteratorMRPublisherForAsync((iterator, publisher) -> {
            cb.setIterator(iterator);
            return publisher.asyncPub(topic, data, messageAttribute, cb);
        });
    }

    @Override
    public boolean asyncPubDelay(String topic, byte[] data, int sec, MessageCallback callback) {
        GuaranteeMessageCallback cb = new GuaranteeMessageCallback(null, Way.AsyncPubDelay,
                new MessageContext(topic, data).buildSec(sec).buildPriority(priority), callback);
        return iteratorMRPublisherForAsync((iterator, publisher) -> {
            cb.setIterator(iterator);
            return publisher.asyncPubDelay(topic, data, sec, cb);
        });
    }

    @Override
    public boolean asyncPubSort(String topic, byte[] data, String sortKey, MessageCallback callback) {
        GuaranteeMessageCallback cb = new GuaranteeMessageCallback(null, Way.AsyncPubSort,
                new MessageContext(topic, data).buildSortKey(sortKey).buildPriority(priority), callback);
        return iteratorMRPublisherForAsync((iterator, publisher) -> {
            cb.setIterator(iterator);
            return publisher.asyncPubSort(topic, data, sortKey, cb);
        });
    }

    @Override
    public boolean asyncPubDirect(String topic, byte[] data, MessageCallback callback) {
        GuaranteeMessageCallback cb = new GuaranteeMessageCallback(null, Way.AsyncPubDirect,
                new MessageContext(topic, data).buildPriority(priority), callback);
        return iteratorMRPublisherForAsync((iterator, publisher) -> {
            cb.setIterator(iterator);
            return publisher.asyncPubDirect(topic, data, cb);
        });
    }

    @Override
    public boolean unsafePub(String topic, byte[] data) {
        return iteratorMRPublisherForBoolean(publisher -> publisher.unsafePub(topic, data));
    }


    enum Way {
        AsyncPub,
        AsyncPubDelay,
        AsyncPubSort,
        AsyncPubDirect,
        AsyncPubMessageAttribute
    }

    static class MessageContext {
        private String topic;
        private byte[] data;

        private int sec;
        private String sortKey;
        private int priority;

        private MessageAttribute messageAttribute;

        public MessageContext(String topic, byte[] data) {
            this.topic = topic;
            this.data = data;
        }

        public MessageContext buildSec(int sec) {
            this.sec = sec;
            return this;
        }

        public MessageContext buildSortKey(String sortKey) {
            this.sortKey = sortKey;
            return this;
        }

        public MessageContext buildPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public MessageContext buildMessageAttribute(MessageAttribute messageAttribute) {
            this.messageAttribute = messageAttribute;
            return this;
        }

        public String getTopic() {
            return topic;
        }

        public byte[] getData() {
            return data;
        }

        public int getSec() {
            return sec;
        }

        public String getSortKey() {
            return sortKey;
        }

        public int getPriority() {
            return priority;
        }
    }

    static class GuaranteeMessageCallback implements MessageCallback {

        private Iterator<MRConnectionManager> iterator;
        private Way way;
        private MessageContext messageContext;
        private MessageCallback callback;

        public GuaranteeMessageCallback(Iterator<MRConnectionManager> iterator, Way way, MessageContext messageContext, MessageCallback callback) {
            this.iterator = iterator;
            this.way = way;
            this.messageContext = messageContext;
            this.callback = callback;
        }

        public void setIterator(Iterator<MRConnectionManager> iterator) {
            this.iterator = iterator;
        }

        @Override
        public void on(int status, long id, byte[] bytes) {
            if (status != 0) {
                logger.error("MRConnectionManager Send Error Retry id:{} status:{} ", id, status);
                if (iterator.hasNext()) {
                    MRPublisher publisher = iterator.next().getMRPublisher(messageContext.priority);
                    publisher.setPriority(messageContext.priority);

                    switch (way) {
                        case AsyncPub:
                            publisher.asyncPub(messageContext.getTopic(),
                                    messageContext.getData(), GuaranteeMessageCallback.this);
                            break;
                        case AsyncPubDelay:
                            publisher.asyncPubDelay(messageContext.getTopic(),
                                    messageContext.getData(), messageContext.getSec(), GuaranteeMessageCallback.this);
                            break;
                        case AsyncPubDirect:
                            publisher.asyncPubDirect(messageContext.getTopic(),
                                    messageContext.getData(), GuaranteeMessageCallback.this);
                        case AsyncPubSort:
                            publisher.asyncPubSort(messageContext.getTopic(),
                                    messageContext.getData(), messageContext.getSortKey(), GuaranteeMessageCallback.this);
                            break;
                        case AsyncPubMessageAttribute:
                            publisher.asyncPub(messageContext.getTopic(), messageContext.getData(),
                                    messageContext.messageAttribute, GuaranteeMessageCallback.this);
                            break;
                        default:
                            break;
                    }
                }else{
                    callback.on(status, id, bytes);
                }
            }else{
                callback.on(status, id, bytes);
            }
        }
    }
}

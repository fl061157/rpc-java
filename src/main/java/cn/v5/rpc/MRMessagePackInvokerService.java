package cn.v5.rpc;

import cn.v5.mr.MRMessageListener;
import cn.v5.mr.MRSubscriber;
import cn.v5.rpc.dispatcher.DefaultDispatcherBuilder;
import cn.v5.rpc.dispatcher.Dispatcher;
import cn.v5.rpc.dispatcher.DispatcherBuilder;
import cn.v5.rpc.dispatcher.DispatcherDescription;
import cn.v5.rpc.error.GiveupException;
import cn.v5.rpc.error.RPCError;
import cn.v5.rpc.message.MessageSendable;
import cn.v5.rpc.message.Messages;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.msgpack.MessagePack;
import org.msgpack.type.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MRMessagePackInvokerService implements MRMessageListener {
    private static Logger logger = LoggerFactory.getLogger(MRMessagePackInvokerService.class);

    private String serviceUrl;
    private Class serviceInterface;
    private Object service;

    private Dispatcher dispatcher;
    DispatcherBuilder dispatcherBuilder = new DefaultDispatcherBuilder();
    private MessagePack messagePack;

    private Method[] methods;
    private Map<String, String> namesMap;

    private MetricRegistry registry = null;
    private Map<String, Timer> rpcSubTimerMap = new ConcurrentHashMap<>();
    private Map<String, Counter> rpcSubCounterMap = new ConcurrentHashMap<>();

    public MRMessagePackInvokerService(String topic, Class serviceInterface, Object service, MessagePack messagePack) {
        this.serviceUrl = topic;
        this.serviceInterface = serviceInterface;
        this.service = service;
        this.messagePack = messagePack;
        init();
    }

    public MRMessagePackInvokerService(String topic, Object service, MessagePack messagePack, Map<String, String> namesMap) {
        this.serviceUrl = topic;
        this.service = service;
        this.messagePack = messagePack;
        this.namesMap = namesMap;
        init();
    }

    public MRMessagePackInvokerService(String topic, Object service, MessagePack messagePack, Method[] methods, Map<String, String> namesMap) {
        this.serviceUrl = topic;
        this.service = service;
        this.messagePack = messagePack;
        this.namesMap = namesMap;
        this.methods = methods;
        init();
    }

    public void setRegistry(MetricRegistry registry) {
        this.registry = registry;
    }

    private void init() {
        if (messagePack == null) {
            messagePack = new MessagePack();
        }
        if (serviceInterface != null) {
            dispatcher = dispatcherBuilder.build(service, serviceInterface, this.messagePack);
        } else {
            if (methods == null) {
                dispatcher = dispatcherBuilder.build(service, this.messagePack, namesMap);
            } else {
                dispatcher = dispatcherBuilder.build(service, this.messagePack, methods, namesMap);
            }
        }
//        DispatcherDescription dd = dispatcher.getDispatcherDescription();
//        logger.info("RPC Service DispatcherDescription:{}", dd);
//        LangTemplate lt = new JavaSyncTemplateImpl();
//        logger.info("sync java srouce :{}", lt.source(this.serviceUrl, dd));
//
//        LangTemplate lt2 = new JavaAsyncTemplateImpl();
//        logger.info("async java srouce :{}", lt2.source(this.serviceUrl, dd));
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public DispatcherDescription getDispatcherDescription() {
        if (dispatcher == null) {
            return null;
        }
        return dispatcher.getDispatcherDescription();
    }

    @Override
    public void onMessage(MRSubscriber subscriber, long messageId, byte[] data) {
        Timer subTimer = null;
        Timer.Context ctx = null;
        Counter subCounter = null;
        if (registry != null) {
            String key = getServiceUrl();
            subTimer = rpcSubTimerMap.get(key);
            if (subTimer == null) {
                synchronized (rpcSubTimerMap) {
                    subTimer = rpcSubTimerMap.get(key);
                    if (subTimer == null) {
                        subTimer = registry.timer(MetricRegistry.name("rpc-sub", key));
                        rpcSubTimerMap.put(key, subTimer);
                    }
                }
            }
            subCounter = rpcSubCounterMap.get(getServiceUrl());
            if (subCounter == null) {
                synchronized (rpcSubCounterMap) {
                    subCounter = rpcSubCounterMap.get(getServiceUrl());
                    if (subCounter == null) {
                        subCounter = registry.counter(MetricRegistry.name("rpc-sub-counter", key));
                        rpcSubCounterMap.put(key, subCounter);
                    }
                }
            }
            ctx = subTimer.time();
            subCounter.inc();
        }
        try {
            //logger.debug("recv request data size = {}", data.length);
            MessageSendable messageSendable = new MessageSendableSubImpl(subscriber, messageId, this.messagePack);
            Value msg = messagePack.read(data);
            handleMessageImpl(messageSendable, msg);
        } catch (GiveupException e) {
            logger.warn("giveup : {}", e.getMessage());
            subscriber.ackFail(messageId);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            subscriber.ackFail(messageId);
        } finally {
            if (ctx != null) {
                ctx.stop();
            }
            if (subCounter != null) {
                subCounter.dec();
            }
        }
    }

    protected void handleMessageImpl(MessageSendable channel, Value msg) {
        Value[] array = msg.asArrayValue().getElementArray();

        // TODO check array.length
        int type = array[0].asIntegerValue().getInt();
        if (type == Messages.REQUEST) {
            // REQUEST
            int msgid = array[1].asIntegerValue().getInt();
            String method = array[2].asRawValue().getString();
            Value args = array[3];
            onRequest(channel, msgid, method, args);
        } else if (type == Messages.REQUEST_WITH_TRACEID) {
            // REQUEST
            String traceId = array[1].asRawValue().getString();
            MDC.put(Messages.TRACE_ID, traceId);
            try {
                int msgid = array[2].asIntegerValue().getInt();
                String method = array[3].asRawValue().getString();
                Value args = array[4];
                onRequest(channel, msgid, method, args);
            } finally {
                MDC.remove(Messages.TRACE_ID);
            }
        } else if (type == Messages.NOTIFY) {
            // NOTIFY
            String method = array[1].asRawValue().getString();
            Value args = array[2];
            onNotify(method, args);
            channel.sendMessage(null);
        } else if (type == Messages.NOTIFY_WITH_TRACEID) {
            // NOTIFY
            String traceId = array[1].asRawValue().getString();
            MDC.put(Messages.TRACE_ID, traceId);
            try {
                String method = array[2].asRawValue().getString();
                Value args = array[3];
                onNotify(method, args);
                channel.sendMessage(null);
            } finally {
                MDC.remove(Messages.TRACE_ID);
            }
        } else {
            // FIXME error result
            throw new RuntimeException("unknown message type: " + type);
        }
    }

    protected void onNotify(String method, Value args) {
        Request request = new Request(method, args);
        try {
            dispatcher.dispatch(request);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            // FIXME ignore?
        }
    }

    protected void onRequest(MessageSendable channel, int msgid, String method, Value args) {
        Request request = new Request(channel, msgid, method, args);
        try {
            logger.debug("on request method : {}", method);
            dispatcher.dispatch(request);
        } catch (GiveupException e) {
            throw e;
        } catch (RPCError e) {
            // FIXME
            request.sendError(e.getCode(), e);
        } catch (Exception e) {
            logger.error("Unexpected error occured while calling : " + method, e);
            // FIXME request.sendError("RemoteError", e.getMessage());
            if (e.getMessage() == null) {
                request.sendError(e.getClass().getName() + " : (empty)");
            } else {
                request.sendError(e.getClass().getName() + " : " + e.getMessage());
            }
        }
    }

    static class MessageSendableSubImpl implements MessageSendable {
        private long messageId;
        private MRSubscriber subscriber;
        private MessagePack messagePack;

        public MessageSendableSubImpl(MRSubscriber subscriber, long messageId, MessagePack messagePack) {
            this.subscriber = subscriber;
            this.messagePack = messagePack;
            this.messageId = messageId;
        }

        @Override
        public void sendMessage(Object obj) {
            if (obj == null) {
                subscriber.ackOk(this.messageId);
            } else {
                try {
                    byte[] bytes = messagePack.write(obj);
                    if (logger.isDebugEnabled()) {
                        if (bytes != null && bytes.length > 64000) {
                            logger.debug("ack message big size = {}, object = {}", bytes.length, obj);
                        }
                    }
                    subscriber.ackOk(this.messageId, bytes);
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }
}

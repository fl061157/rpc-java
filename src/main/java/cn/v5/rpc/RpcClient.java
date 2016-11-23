package cn.v5.rpc;
//
// MessagePack-RPC for Java
//
// Copyright (C) 2010 FURUHASHI Sadayuki
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//

import cn.v5.mr.MRPublisher;
import cn.v5.mr.MessageAttribute;
import cn.v5.mr.MessageResultContext;
import cn.v5.rpc.error.TransportError;
import cn.v5.rpc.message.Messages;
import cn.v5.rpc.message.NotifyMessage;
import cn.v5.rpc.message.RequestMessage;
import cn.v5.rpc.reflect.ProxyMethodInterceptor;
import cn.v5.rpc.reflect.Reflect;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.msgpack.MessagePack;
import org.msgpack.template.Template;
import org.msgpack.type.Value;
import org.msgpack.type.ValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RpcClient {
    private static Logger logger = LoggerFactory.getLogger(RpcClient.class);

    private Reflect reflect;
    private MessagePack messagePack;

    private int requestTimeout;

    private MRPublisher publisher;

    private Executor executor;

    private MetricRegistry registry;

    private Map<String, Timer> requestTimerMap = new ConcurrentHashMap<>();
    private Map<String, Counter> requestCurrentCounterMap = new ConcurrentHashMap<>();

    public RpcClient(MessagePack messagePack, MRPublisher publisher, Executor executor, int requestTimeout, MetricRegistry registry) {
        this.messagePack = messagePack;
        this.reflect = new Reflect(this.messagePack);
        this.requestTimeout = requestTimeout;
        this.publisher = publisher;
        this.executor = executor;
        this.registry = registry;
    }

    public RpcClient(MessagePack messagePack, MRPublisher publisher, Executor executor, int requestTimeout) {
        this(messagePack, publisher, executor, requestTimeout, null);
    }

    public RpcClient(MessagePack messagePack, MRPublisher publisher, Executor executor) {
        this(messagePack, publisher, executor, 0);
    }

    public <T> T proxy(String topic, Class<T> iface, ProxyMethodInterceptor methodInterceptor) {
        return reflect.getProxy(topic, iface).newProxyInstance(this, methodInterceptor);
    }

    public void setPublisher(MRPublisher publisher) {
        this.publisher = publisher;
    }

    public void setRegistry(MetricRegistry registry) {
        this.registry = registry;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Executor getExecutor() {
        return executor;
    }

    private <V> V getResult(RpcFuture<V> f, String topic, String method) {
        while (true) {
            try {
                if (requestTimeout <= 0) {
                    return f.get();
                } else {
                    return f.get(requestTimeout, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            } catch (TimeoutException e) {
                // FIXME
                throw new RuntimeException("Time out to call topic :" + topic + ",method:" + method, e);
            }
        }
    }

    public Value callApply(String topic, String method, Object... args) {
        RpcFuture<Value> f = sendRequest(Value.class, true, topic, method, args);
        return getResult(f, topic, method);
    }

    public <V> V callApply(Class<V> clazz, String topic, String method, Object... args) {
        RpcFuture<V> f = sendRequest(clazz, true, topic, method, args);
        return getResult(f, topic, method);
    }

    public RpcFuture<Value> callAsyncApply(String topic, String method, Object... args) {
        return sendRequest(Value.class, false, topic, method, args);
    }

    public <V> RpcFuture<V> callAsyncApply(Class<V> clazz, String topic, String method, Object... args) {
        return sendRequest(clazz, false, topic, method, args);
    }

    public <V> RpcFuture<V> callAsyncApply(Template<V> resultTemplate, String topic, String method, Object... args) {
        return sendRequest(resultTemplate, false, topic, method, args);
    }

    public void notifyApply(String topic, String method, Object... args) {
        sendNotify(0, 0, topic, method, args);
    }

    public void delayNotifyApply(int sec, String topic, String method, Object... args) {
        sendNotify(sec, 0, topic, method, args);
    }

    public void aliveNotifyApply(int sec, String topic, String method, Object... args) {
        sendNotify(0, sec, topic, method, args);
    }

    public void delayAndAliveNotifyApply(int delay_sec, int alive_sec, String topic, String method, Object... args) {
        sendNotify(delay_sec, alive_sec, topic, method, args);
    }

    private <V> RpcFuture<V> sendRequest(Class<V> clazz, boolean sync, String topic, String method, Object[] args) {
        Template<V> resultTemplate = null;
        if (clazz != void.class && clazz != Value.class) {
            resultTemplate = messagePack.lookup(clazz);
        }
        return sendRequest(resultTemplate, sync, topic, method, args);
    }

    private <V> RpcFuture<V> sendRequest(Template<V> resultTemplate, boolean sync, String topic, String method, Object[] args) {
        RequestMessage msg = new RequestMessage(sync, 0, method, args);
        RpcFutureAdapter f = null;
        Timer rpcMetricsTimer = null;
        Counter rpcCurrentCounter = null;
        if (registry != null) {
            String key = getRpcMetricsName(topic, method);
            rpcMetricsTimer = requestTimerMap.get(key);
            if (rpcMetricsTimer == null) {
                synchronized (requestTimerMap) {
                    rpcMetricsTimer = requestTimerMap.get(key);
                    if (rpcMetricsTimer == null) {
                        rpcMetricsTimer = registry.timer(MetricRegistry.name("rpc-call", topic, method));
                        requestTimerMap.put(key, rpcMetricsTimer);
                    }
                }
            }
            rpcCurrentCounter = requestCurrentCounterMap.get(key);
            if (rpcCurrentCounter == null) {
                synchronized (requestCurrentCounterMap) {
                    if (rpcCurrentCounter == null) {
                        rpcCurrentCounter = registry.counter(MetricRegistry.name("rpc-call-counter", topic, method));
                        requestCurrentCounterMap.put(key, rpcCurrentCounter);
                    }
                }
            }
        }
        if (sync) {
            f = new RpcFutureAdapterSyncImpl(executor);
        } else {
            f = new RpcFutureAdapterAsyncImpl(getRequestTimeout(), executor);
        }
        RpcFuture<V> ret = new RpcFuture<>(messagePack, f, resultTemplate);
        if (rpcMetricsTimer != null && rpcCurrentCounter != null) {
            final Counter tempCounter = rpcCurrentCounter;
            tempCounter.inc();
            final Timer.Context ctx = rpcMetricsTimer.time();
            ret.then(() -> {
                ctx.stop();
                tempCounter.dec();
            });
            ret.onError(e -> {
                ctx.stop();
                tempCounter.dec();
            });
        }

        sendMessage(sync, topic, f, msg);

        return ret;
    }

    private String getRpcMetricsName(String topic, String method) {
        StringBuilder sb = new StringBuilder();
        sb.append(topic).append(".").append(method);
        return sb.toString();
    }

    private void sendMessage(boolean sync, String topic, RpcFutureAdapter f, RequestMessage msg) {
        try {

            //JSON json = new JSON(messagePack);
            //byte[] dd = json.write(msg);
            //logger.debug("\njson:{}", new String(dd, "UTF-8"));

            byte[] data = messagePack.write(msg);
            //logger.debug("pub direct data size {}", data.length);
            //logger.debug("pub data : {} ", Hex.encodeHexString(data));
            MessageAttribute messageAttribute = new MessageAttribute();
            messageAttribute.setAckType(MessageAttribute.AckType.PEER);
            messageAttribute.setQosType(MessageAttribute.QosType.NONE);
            messageAttribute.setAlive(this.requestTimeout > 0 ? this.requestTimeout - 1 : 0);
            if (sync) {
                MessageResultContext mrc = publisher.syncPubForAck(topic, data, messageAttribute);
                if (mrc.getResult() != 0 || mrc.getBytes() == null || mrc.getBytes().length < 1) {
                    logger.error("Result:{} Bytes:{}  ", mrc.getResult(), mrc.getBytes());
                    throw new TransportError("sync send err.");
                }
                //logger.debug("recv data : {} ", Hex.encodeHexString(mrc.getBytes()));
                onResponseMessageBytes(f, mrc.getBytes());

            } else {
                boolean ret = publisher.asyncPub(topic, data, messageAttribute, (status, mid, bytes) -> {
                    if (status != 0) {
                        f.setResult(null, ValueFactory.createRawValue("rpc async send error."));
                    } else if (bytes == null || bytes.length < 1) {
                        f.setResult(null, ValueFactory.createRawValue("rpc async recv data is null."));
                    } else {
                        onResponseMessageBytes(f, bytes);
                    }
                });
                if (!ret) {
                    throw new TransportError("async send err.");
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new TransportError("async send err : " + e.getMessage());
        }
    }

    private void onResponseMessageBytes(RpcFutureAdapter f, byte[] data) {
        try {
            if (data == null || data.length == 1) {
                f.setResult(null, ValueFactory.createRawValue("Exception: response data error."));
            } else {
                Value msg = messagePack.read(data);
                handleMessageImpl(f, msg);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            // FIXME
            f.setResult(null, ValueFactory.createRawValue("Exception:" + e.getMessage()));
        }
    }

    private void handleMessageImpl(RpcFutureAdapter f, Value msg) {
        Value[] array = msg.asArrayValue().getElementArray();

        // TODO check array.length
        int type = array[0].asIntegerValue().getInt();
        if (type == Messages.RESPONSE) {
            //int msgid = array[1].asIntegerValue().getInt();
            Value error = array[2];
            Value result = array[3];
            f.setResult(result, error);
        } else {
            // FIXME error result
            // throw new RuntimeException("unknown message type: " + type);
            throw new TransportError("unknown message type " + type);
        }
    }

    private void sendNotify(int delay_sec, int alive_sec, String topic, String method, Object[] args) {
        NotifyMessage msg = new NotifyMessage(topic, method, args);
        try {
            byte[] data = messagePack.write(msg);

            MessageAttribute messageAttribute = new MessageAttribute();
            messageAttribute.setAlive(alive_sec);
            messageAttribute.setDelay(delay_sec);

            boolean ret;
            ret = publisher.asyncPub(topic, data, messageAttribute, (status, mid, bytes) -> {
                if (status != 0) {
                    logger.error("async pub error status : {}, topic : {}, method : {}, message attribute : {}",
                            status, topic, method, messageAttribute);
                } else {
                    logger.debug("async pub success, topic : {}, method : {}, mid : {}",
                            topic, method, mid);
                }
                //TODO
            });
            if (!ret) {
                throw new TransportError("async send notify err.");
            }
        } catch (IOException e) {
            //logger.error(e.getMessage(), e);
            throw new TransportError("async send err :" + e.getMessage(), e);
        }
    }

    void close() {
        //TODO
    }
}

package cn.v5.rpc.zeromq;

import cn.v5.mr.MRPublisher;
import cn.v5.mr.MessageAttribute;
import cn.v5.mr.MessageCallback;
import cn.v5.mr.MessageResultContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

public class ZeroMQPublisher implements MRPublisher {

    private static Logger logger = LoggerFactory.getLogger(ZeroMQPublisher.class);

    ZeroMQRequest zeroMQRequest;

    public void setZeroMQRequest(ZeroMQRequest zeroMQRequest) {
        this.zeroMQRequest = zeroMQRequest;
    }

    @Override
    public void setPriority(int priority) {

    }

    @Override
    public boolean syncPub(String topic, byte[] data) {
        return false;
    }

    @Override
    public boolean syncPub(String topic, byte[] data, MessageAttribute messageAttribute) {
        return false;
    }

    @Override
    public MessageResultContext syncPubForAck(String topic, byte[] data, MessageAttribute messageAttribute) {
        return null;
    }

    @Override
    public boolean syncPubDelay(String topic, byte[] data, int sec) {
        return false;
    }

    @Override
    public boolean syncPubSort(String topic, byte[] data, String sortKey) {
        return false;
    }

    @Override
    public MessageResultContext syncPubDirect(String topic, byte[] data) {
        byte[] ret = zeroMQRequest.syncSend(ZeroMQUtils.getRequestData(topic, data));
        return new MessageResultContext(0, 0, null, ret);
    }

    @Override
    public Future<MessageResultContext> asyncPub(String topic, byte[] data) {
        return null;
    }

    @Override
    public boolean asyncPub(String topic, byte[] data, MessageCallback callback) {
        return false;
    }

    @Override
    public boolean asyncPub(String topic, byte[] data, MessageAttribute messageAttribute, MessageCallback callback) {
        return false;
    }

    @Override
    public boolean asyncPubDelay(String topic, byte[] data, int sec, MessageCallback callback) {
        return false;
    }

    @Override
    public boolean asyncPubSort(String topic, byte[] data, String sortKey, MessageCallback callback) {
        return false;
    }

    @Override
    public boolean asyncPubDirect(String topic, byte[] data, MessageCallback callback) {
        zeroMQRequest.asyncSend(ZeroMQUtils.getRequestData(topic, data), ret -> callback.on(0, 0, ret));
        return true;
    }

    @Override
    public boolean unsafePub(String topic, byte[] data) {
        return false;
    }
}

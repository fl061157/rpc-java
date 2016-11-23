package cn.v5.rpc.test;

import cn.v5.mr.*;
import cn.v5.rpc.MRMessagePackInvokerServiceExporter;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;

public class MsgLocalPublisherImpl implements MRPublisher {

    private Executor executor;

    private MRMessagePackInvokerServiceExporter mrMessagePackInvokerServiceExporter;

    public void setMrMessagePackInvokerServiceExporter(MRMessagePackInvokerServiceExporter mrMessagePackInvokerServiceExporter) {
        this.mrMessagePackInvokerServiceExporter = mrMessagePackInvokerServiceExporter;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void setPriority(int priority) {

    }

    @Override
    public boolean syncPub(String topic, byte[] data  ) {
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
    public boolean syncPubDelay(String topic, byte[] data, int sec ) {
        return false;
    }

    @Override
    public boolean syncPubSort(String topic, byte[] data, String sortKey ) {
        return false;
    }

    @Override
    public MessageResultContext syncPubDirect(String topic, byte[] data ) {
        MessageResultContext mrc = new MessageResultContext(0, 1, null, null);
        mrMessagePackInvokerServiceExporter.getMrMessagePackInvokerService().onMessage(new MRSubscriber() {
            @Override
            public void ackOk(long messageId) {

            }

            @Override
            public void ackOk(long messageId, byte[] data) {
                mrc.setBytes(data);
            }

            @Override
            public void ackFail(long messageId) {

            }

            @Override
            public void unSub() {

            }

            @Override
            public void on(int status, long mid, byte[] bytes) {

            }
        }, 1, data);
        return mrc;
    }

    @Override
    public Future<MessageResultContext> asyncPub(String topic, byte[] data ) {
        return null;
    }

    @Override
    public boolean asyncPub(String topic, byte[] data, MessageCallback callback ) {
        return false;
    }

    @Override
    public boolean asyncPub(String topic, byte[] data, MessageAttribute messageAttribute, MessageCallback callback ) {
        return false;
    }

    @Override
    public boolean asyncPubDelay(String topic, byte[] data, int sec, MessageCallback callback ) {
        return false;
    }

    @Override
    public boolean asyncPubSort(String topic, byte[] data, String sortKey, MessageCallback callback ) {
        return false;
    }

    @Override
    public boolean asyncPubDirect(String topic, byte[] data, MessageCallback callback ) {
        MessageResultContext mrc = syncPubDirect(topic, data );
        executor.execute(() -> callback.on(mrc.getResult(), mrc.getMessageId(), mrc.getBytes()));
        return true;
    }

    @Override
    public boolean unsafePub(String topic, byte[] data ) {
        return false;
    }
}
